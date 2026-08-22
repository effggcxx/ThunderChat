package me.ehsan.thunderchat.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.Base64;

/** Async persistence/cache layer with MySQL as the shared backend and YAML as a transparent fallback. */
public final class MySqlStorage {
    private final ThunderChat plugin;
    private final ExecutorService io = Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "ThunderChat-Storage"); t.setDaemon(true); return t; });
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "ThunderChat-StorageScheduler"); t.setDaemon(true); return t; });
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, String> dirty = new ConcurrentHashMap<>();
    private final Set<String> dirtyDeletes = ConcurrentHashMap.newKeySet();
    private final File yamlDirectory;
    private volatile HikariDataSource dataSource;
    private volatile boolean enabled;
    private volatile boolean yamlFallback;
    private volatile ScheduledFuture<?> flushFuture;

    public MySqlStorage(ThunderChat plugin) {
        this.plugin = plugin;
        String type = plugin.getPluginConfig().getString("storage.type", "mysql");
        yamlFallback = "yaml".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.fallback", "yaml")) || "yaml".equalsIgnoreCase(type);
        yamlDirectory = new File(plugin.getDataFolder(), "storage-fallback");
        if ("mysql".equalsIgnoreCase(type)) connect();
        else { enabled = false; plugin.getLogger().info("ThunderChat storage is configured to use YAML fallback storage."); }
    }
    private void connect() {
        String host=plugin.getPluginConfig().getString("storage.mysql.host","127.0.0.1"); int port=plugin.getPluginConfig().getInt("storage.mysql.port",3306); String database=plugin.getPluginConfig().getString("storage.mysql.database","thunderchat"); String user=plugin.getPluginConfig().getString("storage.mysql.username","root"); String password=plugin.getPluginConfig().getString("storage.mysql.password","");
        if(!database.matches("[A-Za-z0-9_$-]+")){disableWithFallback("Invalid storage.mysql.database name: "+database);return;}
        try{
            HikariConfig bootstrap=new HikariConfig();bootstrap.setJdbcUrl(jdbcUrl(host,port,""));bootstrap.setUsername(user);bootstrap.setPassword(password);bootstrap.setMaximumPoolSize(1);bootstrap.setMinimumIdle(0);bootstrap.setConnectionTimeout(5000);bootstrap.setInitializationFailTimeout(5000);bootstrap.setPoolName("ThunderChat-MySQL-Bootstrap");
            try(HikariDataSource pool=new HikariDataSource(bootstrap);Connection c=pool.getConnection();Statement s=c.createStatement()){s.executeUpdate("CREATE DATABASE IF NOT EXISTS `"+database+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");}
            HikariConfig config=new HikariConfig();config.setJdbcUrl(jdbcUrl(host,port,database));config.setUsername(user);config.setPassword(password);config.setMaximumPoolSize(Math.max(2,plugin.getPluginConfig().getInt("storage.mysql.pool-size",4)));config.setMinimumIdle(1);config.setConnectionTimeout(5000);config.setValidationTimeout(3000);config.setIdleTimeout(600000);config.setMaxLifetime(1800000);config.setPoolName("ThunderChat-MySQL");
            dataSource=new HikariDataSource(config);initialize();enabled=true;plugin.getLogger().info("MySQL storage enabled. Database '"+database+"' is ready.");
        }catch(Exception ex){disableWithFallback("MySQL is unavailable: "+ex.getMessage());}
    }
    private void disableWithFallback(String reason){enabled=false;if(dataSource!=null){try{dataSource.close();}catch(Exception ignored){}dataSource=null;}if(yamlFallback){plugin.getLogger().severe(ChatColor.RED+"[ThunderChat] WARNING: MySQL is unavailable. "+reason);plugin.getLogger().severe(ChatColor.RED+"[ThunderChat] Falling back to local asynchronous YAML storage.");}else{plugin.getLogger().severe(ChatColor.RED+"[ThunderChat] WARNING: MySQL is unavailable and storage.fallback is disabled.");plugin.getLogger().severe(ChatColor.RED+"[ThunderChat] Reason: "+reason);}}
    private String jdbcUrl(String host,int port,String database){return "jdbc:mysql://"+host+":"+port+(database.isEmpty()?"":"/"+database)+"?useSSL=false&characterEncoding=utf8mb4&serverTimezone=UTC&allowPublicKeyRetrieval=true";}
    private void initialize() throws SQLException{try(Connection c=dataSource.getConnection();Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS thunderchat_store (namespace VARCHAR(64) NOT NULL, record_key VARCHAR(191) NOT NULL, value LONGTEXT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(namespace, record_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");s.executeUpdate("CREATE TABLE IF NOT EXISTS thunderchat_network_queue (id BIGINT AUTO_INCREMENT PRIMARY KEY, target_server VARCHAR(128) NOT NULL, payload LONGBLOB NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, consumed TINYINT(1) NOT NULL DEFAULT 0, INDEX(target_server, created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");}}
    public boolean isEnabled(){return enabled&&dataSource!=null&&!dataSource.isClosed();}
    public boolean isUsingYamlFallback(){return !isEnabled()&&yamlFallback;}
    private String id(String namespace,String key){return namespace+"\u0000"+key;}
    private Path yamlPath(String namespace,String key){String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString((namespace+"\u0000"+key).getBytes(StandardCharsets.UTF_8));return new File(yamlDirectory,encoded+".dat").toPath();}
    public CompletableFuture<String> load(String namespace,String key){String id=id(namespace,key);String cached=cache.get(id);if(cached!=null)return CompletableFuture.completedFuture(cached);if(!isEnabled()){if(!yamlFallback)return CompletableFuture.completedFuture(null);return CompletableFuture.supplyAsync(()->readYaml(id,yamlPath(namespace,key)),io);}return CompletableFuture.supplyAsync(()->{try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT value FROM thunderchat_store WHERE namespace=? AND record_key=?")){ps.setString(1,namespace);ps.setString(2,key);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;String value=rs.getString(1);cache.put(id,value);return value;}}catch(SQLException e){plugin.getLogger().warning("MySQL read failed: "+e.getMessage());return yamlFallback?readYaml(id,yamlPath(namespace,key)):null;}},io);}
    private String readYaml(String id,Path path){try{if(!Files.exists(path))return null;String value=Files.readString(path);cache.put(id,value);return value;}catch(Exception e){plugin.getLogger().warning("YAML storage read failed: "+e.getMessage());return null;}}
    public void put(String namespace,String key,String value){String id=id(namespace,key);cache.put(id,value);dirtyDeletes.remove(id);dirty.put(id,value);if(isEnabled())scheduleFlush();else if(yamlFallback)scheduleYamlFlush();}
    public void delete(String namespace,String key){String id=id(namespace,key);cache.remove(id);dirty.remove(id);dirtyDeletes.add(id);if(isEnabled())scheduleFlush();else if(yamlFallback)scheduleYamlFlush();}
    private synchronized void scheduleFlush(){if(flushFuture!=null&&!flushFuture.isDone())return;flushFuture=scheduler.schedule(this::flush,1500,TimeUnit.MILLISECONDS);}
    private synchronized void scheduleYamlFlush(){if(flushFuture!=null&&!flushFuture.isDone())return;flushFuture=scheduler.schedule(this::flushYaml,500,TimeUnit.MILLISECONDS);}
    public void flush(){if(!isEnabled()){if(yamlFallback)flushYaml();return;}if(dirty.isEmpty()&&dirtyDeletes.isEmpty())return;Map<String,String> batch=new HashMap<>(dirty);Set<String> deletes=new HashSet<>(dirtyDeletes);dirty.clear();dirtyDeletes.clear();io.execute(()->{try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try(PreparedStatement upsert=c.prepareStatement("INSERT INTO thunderchat_store(namespace,record_key,value) VALUES(?,?,?) ON DUPLICATE KEY UPDATE value=VALUES(value)")){for(Map.Entry<String,String> e:batch.entrySet()){String[] p=e.getKey().split("\\u0000",2);upsert.setString(1,p[0]);upsert.setString(2,p[1]);upsert.setString(3,e.getValue());upsert.addBatch();}upsert.executeBatch();}try(PreparedStatement del=c.prepareStatement("DELETE FROM thunderchat_store WHERE namespace=? AND record_key=?")){for(String key:deletes){String[] p=key.split("\\u0000",2);del.setString(1,p[0]);del.setString(2,p[1]);del.addBatch();}del.executeBatch();}c.commit();}catch(SQLException e){plugin.getLogger().warning("MySQL batch write failed: "+e.getMessage());dirty.putAll(batch);dirtyDeletes.addAll(deletes);if(yamlFallback)flushYamlEntries(batch,deletes);}});}
    private void flushYaml(){if(!yamlFallback||(dirty.isEmpty()&&dirtyDeletes.isEmpty()))return;Map<String,String> batch=new HashMap<>(dirty);Set<String> deletes=new HashSet<>(dirtyDeletes);dirty.clear();dirtyDeletes.clear();io.execute(()->flushYamlEntries(batch,deletes));}
    private void flushYamlEntries(Map<String,String> batch,Set<String> deletes){try{if(!yamlDirectory.exists()&&!yamlDirectory.mkdirs())throw new IllegalStateException("Could not create "+yamlDirectory);for(Map.Entry<String,String> e:batch.entrySet()){String[] p=e.getKey().split("\\u0000",2);Files.writeString(yamlPath(p[0],p[1]),e.getValue(),StandardCharsets.UTF_8);}for(String key:deletes){String[] p=key.split("\\u0000",2);Files.deleteIfExists(yamlPath(p[0],p[1]));}}catch(Exception e){plugin.getLogger().warning("YAML storage write failed: "+e.getMessage());dirty.putAll(batch);dirtyDeletes.addAll(deletes);}}
    public void enqueueNetwork(String targetServer,byte[] payload){if(!isEnabled()||targetServer==null||targetServer.isBlank()||payload==null)return;io.execute(()->{try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO thunderchat_network_queue(target_server,payload) VALUES(?,?)")){ps.setString(1,targetServer);ps.setBytes(2,payload);ps.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("Could not queue network packet: "+e.getMessage());}});}
    public void drainNetwork(String serverName,Predicate<byte[]> consumer){if(!isEnabled()||serverName==null||serverName.isBlank())return;io.execute(()->{List<Long> ids=new ArrayList<>();List<byte[]> payloads=new ArrayList<>();int retention=Math.max(5,plugin.getPluginConfig().getInt("network.queue-retention-minutes",1440));try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT id,payload FROM thunderchat_network_queue WHERE target_server=? AND consumed=0 AND created_at > (CURRENT_TIMESTAMP - INTERVAL ? MINUTE) ORDER BY id ASC LIMIT 100")){ps.setString(1,serverName);ps.setInt(2,retention);try(ResultSet rs=ps.executeQuery()){while(rs.next()){ids.add(rs.getLong(1));payloads.add(rs.getBytes(2));}}for(int i=0;i<payloads.size();i++){long id=ids.get(i);byte[] payload=payloads.get(i);Bukkit.getScheduler().runTask(plugin,()->{boolean handled;try{handled=consumer.test(payload);}catch(Throwable t){plugin.getLogger().warning("Queued network packet failed: "+t.getMessage());handled=false;}if(handled)io.execute(()->markConsumed(id));});}}catch(SQLException e){plugin.getLogger().warning("Could not drain network queue: "+e.getMessage());}});}
    private void markConsumed(long id){try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE thunderchat_network_queue SET consumed=1 WHERE id=?")){ps.setLong(1,id);ps.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("Could not acknowledge queued network packet: "+e.getMessage());}}
    public void shutdown(){flush();scheduler.shutdownNow();io.shutdown();try{io.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}if(dataSource!=null)dataSource.close();}
}
