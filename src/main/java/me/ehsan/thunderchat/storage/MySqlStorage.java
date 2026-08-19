package me.ehsan.thunderchat.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ehsan.thunderchat.ThunderChat;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async MySQL persistence/cache layer. Gameplay code never waits on JDBC.
 * Values are cached in memory and writes are debounced into short batches.
 */
public final class MySqlStorage {
    private final ThunderChat plugin;
    private final ExecutorService io = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ThunderChat-MySQL");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, String> dirty = new ConcurrentHashMap<>();
    private volatile HikariDataSource dataSource;
    private volatile boolean enabled;
    private volatile ScheduledFuture<?> flushFuture;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ThunderChat-StorageScheduler");
        t.setDaemon(true);
        return t;
    });

    public MySqlStorage(ThunderChat plugin) {
        this.plugin = plugin;
        enabled = "mysql".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.type", "mysql"));
        if (enabled) connect();
    }

    private void connect() {
        try {
            String host = plugin.getPluginConfig().getString("storage.mysql.host", "127.0.0.1");
            int port = plugin.getPluginConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getPluginConfig().getString("storage.mysql.database", "thunderchat");
            String user = plugin.getPluginConfig().getString("storage.mysql.username", "root");
            String password = plugin.getPluginConfig().getString("storage.mysql.password", "");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8mb4&serverTimezone=UTC");
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(Math.max(2, plugin.getPluginConfig().getInt("storage.mysql.pool-size", 4)));
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            config.setPoolName("ThunderChat-MySQL");
            dataSource = new HikariDataSource(config);
            initialize();
            plugin.getLogger().info("MySQL storage enabled with HikariCP.");
        } catch (Exception ex) {
            enabled = false;
            plugin.getLogger().severe("Could not initialize MySQL storage: " + ex.getMessage());
        }
    }

    private void initialize() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS thunderchat_store (namespace VARCHAR(64) NOT NULL, record_key VARCHAR(191) NOT NULL, value LONGTEXT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(namespace, record_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS thunderchat_network_queue (id BIGINT AUTO_INCREMENT PRIMARY KEY, payload LONGBLOB NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, consumed_by TEXT NULL, INDEX(created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    public boolean isEnabled() { return enabled && dataSource != null && !dataSource.isClosed(); }

    private String id(String namespace, String key) { return namespace + "\u0000" + key; }

    public CompletableFuture<String> load(String namespace, String key) {
        String cached = cache.get(id(namespace, key));
        if (cached != null) return CompletableFuture.completedFuture(cached);
        if (!isEnabled()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT value FROM thunderchat_store WHERE namespace=? AND record_key=?")) {
                ps.setString(1, namespace); ps.setString(2, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    String value = rs.getString(1);
                    cache.put(id(namespace, key), value);
                    return value;
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL read failed: " + e.getMessage());
                return null;
            }
        }, io);
    }

    public void put(String namespace, String key, String value) {
        if (!isEnabled()) return;
        String id = id(namespace, key);
        cache.put(id, value);
        dirty.put(id, value);
        scheduleFlush();
    }

    public void delete(String namespace, String key) {
        if (!isEnabled()) return;
        String id = id(namespace, key);
        cache.remove(id); dirty.put(id, null); scheduleFlush();
    }

    private synchronized void scheduleFlush() {
        if (flushFuture != null && !flushFuture.isDone()) return;
        flushFuture = scheduler.schedule(this::flush, 1500, TimeUnit.MILLISECONDS);
    }

    public void flush() {
        if (!isEnabled() || dirty.isEmpty()) return;
        Map<String, String> batch = new ConcurrentHashMap<>();
        batch.putAll(dirty); dirty.keySet().removeAll(batch.keySet());
        io.execute(() -> {
            try (Connection c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (PreparedStatement upsert = c.prepareStatement("INSERT INTO thunderchat_store(namespace,record_key,value) VALUES(?,?,?) ON DUPLICATE KEY UPDATE value=VALUES(value)")) {
                    try (PreparedStatement delete = c.prepareStatement("DELETE FROM thunderchat_store WHERE namespace=? AND record_key=?")) {
                        for (Map.Entry<String,String> e : batch.entrySet()) {
                            String[] parts = e.getKey().split("\\u0000", 2);
                            if (e.getValue() == null) { delete.setString(1, parts[0]); delete.setString(2, parts[1]); delete.addBatch(); }
                            else { upsert.setString(1, parts[0]); upsert.setString(2, parts[1]); upsert.setString(3, e.getValue()); upsert.addBatch(); }
                        }
                        upsert.executeBatch(); delete.executeBatch();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL batch write failed: " + e.getMessage());
                dirty.putAll(batch);
            }
        });
    }

    public void enqueueNetwork(byte[] payload) {
        if (!isEnabled() || payload == null) return;
        io.execute(() -> {
            try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO thunderchat_network_queue(payload) VALUES(?)")) {
                ps.setBytes(1, payload); ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().warning("Could not queue network packet: " + e.getMessage()); }
        });
    }

    public void drainNetwork(String serverName, java.util.function.Consumer<byte[]> consumer) {
        if (!isEnabled() || serverName == null || serverName.isBlank()) return;
        io.execute(() -> {
            try (Connection c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (PreparedStatement ps = c.prepareStatement("SELECT id,payload,COALESCE(consumed_by,'') FROM thunderchat_network_queue WHERE created_at > (CURRENT_TIMESTAMP - INTERVAL 5 MINUTE) ORDER BY id ASC LIMIT 100 FOR UPDATE")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong(1); byte[] payload = rs.getBytes(2); String consumed = rs.getString(3);
                            if (containsServer(consumed, serverName)) continue;
                            try { consumer.accept(payload); } catch (Throwable ignored) { continue; }
                            String next = consumed.isEmpty() ? serverName : consumed + "," + serverName;
                            try (PreparedStatement up = c.prepareStatement("UPDATE thunderchat_network_queue SET consumed_by=? WHERE id=?")) { up.setString(1, next); up.setLong(2, id); up.executeUpdate(); }
                        }
                    }
                }
                c.commit();
            } catch (SQLException e) { plugin.getLogger().warning("Could not drain network queue: " + e.getMessage()); }
        });
    }

    private boolean containsServer(String value, String server) { for (String s : value.split(",")) if (s.equals(server)) return true; return false; }

    public void shutdown() {
        flush();
        scheduler.shutdownNow();
        io.shutdown();
        try { io.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (dataSource != null) dataSource.close();
    }
}
