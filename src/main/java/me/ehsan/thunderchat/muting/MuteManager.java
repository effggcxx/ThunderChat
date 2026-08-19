package me.ehsan.thunderchat.muting;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Stores chat mutes in memory with asynchronous MySQL persistence. */
public final class MuteManager {
    private static final int PROTOCOL_VERSION = 1;
    private final ThunderChat plugin;
    private final Set<String> global = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<UUID>> players = new ConcurrentHashMap<>();
    private volatile boolean localGlobalMuted = false;
    private final Set<UUID> localPlayers = ConcurrentHashMap.newKeySet();
    private final File file;

    public MuteManager(ThunderChat plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "mutes.yml"); for (String c : channels()) players.put(c, ConcurrentHashMap.newKeySet()); load(); }
    private List<String> channels() { return Arrays.asList("global", "donator", "staff", "admin", "highrank"); }
    public boolean isMuted(Player p, String channel) { if ("local".equalsIgnoreCase(channel)) return !p.hasPermission("thunderchat.bypass.mute") && !p.hasPermission("thunderchat.bypass.mute.local") && (localGlobalMuted || localPlayers.contains(p.getUniqueId())); return !p.hasPermission("thunderchat.bypass.mute") && !p.hasPermission("thunderchat.bypass.mute." + channel) && (global.contains(channel) || players.getOrDefault(channel, Collections.emptySet()).contains(p.getUniqueId())); }
    public void setGlobalMuted(String channel, boolean muted) { if ("local".equalsIgnoreCase(channel)) localGlobalMuted = muted; else if (muted) global.add(channel); else global.remove(channel); save(); if (!"local".equalsIgnoreCase(channel)) broadcast(channel, null, muted); }
    public void setPlayerMuted(String channel, UUID id, boolean muted) { if ("local".equalsIgnoreCase(channel)) { if (muted) localPlayers.add(id); else localPlayers.remove(id); } else { players.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()); if (muted) players.get(channel).add(id); else players.get(channel).remove(id); } save(); if (!"local".equalsIgnoreCase(channel)) broadcast(channel, id, muted); }
    private void broadcast(String channel, UUID id, boolean muted) { Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null); if (carrier == null) return; try { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeInt(PROTOCOL_VERSION); d.writeUTF("MUTE"); d.writeUTF(channel); d.writeBoolean(id != null); if (id != null) d.writeUTF(id.toString()); d.writeBoolean(muted); d.flush(); plugin.getNetworkMessenger().forwardAll(carrier, b.toByteArray()); } catch (IOException e) { plugin.getLogger().warning("Could not synchronize chat mute: " + e.getMessage()); } }
    public void onNetworkPacket(String channel, Player source, byte[] data) { if (!"BungeeCord".equals(channel) || !plugin.getPluginConfig().getBoolean("network.enabled", true)) return; try { DataInputStream d = new DataInputStream(new ByteArrayInputStream(data)); if (d.readInt() != PROTOCOL_VERSION || !"MUTE".equals(d.readUTF())) return; String c = d.readUTF(); boolean has = d.readBoolean(); UUID id = has ? UUID.fromString(d.readUTF()) : null; boolean muted = d.readBoolean(); if ("local".equalsIgnoreCase(c)) return; Set<UUID> set = players.computeIfAbsent(c, k -> ConcurrentHashMap.newKeySet()); if (id == null) { if (muted) global.add(c); else global.remove(c); } else { if (muted) set.add(id); else set.remove(id); } save(); } catch (Exception e) { plugin.getLogger().warning("Malformed ThunderChat mute packet: " + e.getMessage()); } }
    public void reload() { load(); }

    private void load() {
        global.clear(); localPlayers.clear(); localGlobalMuted = false; for (String channel : channels()) players.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).clear();
        String serialized = plugin.getStorage().isEnabled() ? plugin.getStorage().load("mutes", "state").join() : null;
        if (serialized == null && file.exists()) { serialized = YamlStorage.serialize(YamlConfiguration.loadConfiguration(file)); if (plugin.getStorage().isEnabled()) plugin.getStorage().put("mutes", "state", serialized); }
        if (serialized == null) return;
        YamlConfiguration data = YamlStorage.parse(serialized); localGlobalMuted = data.getBoolean("local.global", false);
        for (String id : data.getStringList("local.players")) try { localPlayers.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
        global.addAll(data.getStringList("global")); for (String channel : channels()) for (String id : data.getStringList("players." + channel)) try { players.get(channel).add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
    }

    public synchronized void save() {
        YamlConfiguration data = new YamlConfiguration(); data.set("local.global", localGlobalMuted); data.set("local.players", localPlayers.stream().map(UUID::toString).toList()); data.set("global", new ArrayList<>(global));
        for (String channel : channels()) data.set("players." + channel, players.getOrDefault(channel, Collections.emptySet()).stream().map(UUID::toString).toList());
        if (plugin.getStorage().isEnabled()) { plugin.getStorage().put("mutes", "state", YamlStorage.serialize(data)); return; }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save mutes.yml: " + e.getMessage()); }
    }
    public List<String> getChannels() { List<String> all = new ArrayList<>(); all.add("local"); all.addAll(channels()); return all; }
    public Set<String> getGloballyMutedChannels() { Set<String> copy = new LinkedHashSet<>(global); if (localGlobalMuted) copy.add("local"); return Collections.unmodifiableSet(copy); }
    public Map<String, Set<UUID>> getPlayerMutes() { Map<String, Set<UUID>> copy = new LinkedHashMap<>(); if (!localPlayers.isEmpty()) copy.put("local", Set.copyOf(localPlayers)); for (Map.Entry<String, Set<UUID>> entry : players.entrySet()) if (!entry.getValue().isEmpty()) copy.put(entry.getKey(), Set.copyOf(entry.getValue())); return copy; }
}
