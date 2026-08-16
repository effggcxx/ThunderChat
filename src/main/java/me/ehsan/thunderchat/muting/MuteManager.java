package me.ehsan.thunderchat.muting;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;

/** Stores local/network chat mutes and synchronizes network channels safely. */
public final class MuteManager implements PluginMessageListener {
    private static final String SUB = "ThunderChat";
    private static final int PROTOCOL_VERSION = 1;
    private final ThunderChat plugin;
    private final Set<String> global = new HashSet<>();
    private final Map<String, Set<UUID>> players = new HashMap<>();
    private boolean localGlobalMuted = false;
    private final Set<UUID> localPlayers = new HashSet<>();
    private final File file;
    public MuteManager(ThunderChat plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "mutes.yml"); for (String c : channels()) players.put(c, new HashSet<>()); load(); plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this); }
    private List<String> channels() { return Arrays.asList("global", "donator", "staff", "admin", "highrank"); }
    public boolean isMuted(Player p, String channel) { if ("local".equalsIgnoreCase(channel)) return !p.hasPermission("thunderchat.bypass.mute") && !p.hasPermission("thunderchat.bypass.mute.local") && (localGlobalMuted || localPlayers.contains(p.getUniqueId())); return !p.hasPermission("thunderchat.bypass.mute") && !p.hasPermission("thunderchat.bypass.mute." + channel) && (global.contains(channel) || players.getOrDefault(channel, Collections.emptySet()).contains(p.getUniqueId())); }
    public void setGlobalMuted(String channel, boolean muted) { if ("local".equalsIgnoreCase(channel)) localGlobalMuted = muted; else if (muted) global.add(channel); else global.remove(channel); save(); if (!"local".equalsIgnoreCase(channel)) broadcast(channel, null, muted); }
    public void setPlayerMuted(String channel, UUID id, boolean muted) { if ("local".equalsIgnoreCase(channel)) { if (muted) localPlayers.add(id); else localPlayers.remove(id); } else { players.computeIfAbsent(channel, k -> new HashSet<>()); if (muted) players.get(channel).add(id); else players.get(channel).remove(id); } save(); if (!"local".equalsIgnoreCase(channel)) broadcast(channel, id, muted); }
    private void broadcast(String channel, UUID id, boolean muted) { Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null); if (carrier == null) { plugin.getLogger().warning("Could not synchronize chat mute because this backend has no online player to carry the plugin message."); return; } try { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeInt(PROTOCOL_VERSION); d.writeUTF("MUTE"); d.writeUTF(channel); d.writeBoolean(id != null); if (id != null) d.writeUTF(id.toString()); d.writeBoolean(muted); d.flush(); plugin.getNetworkMessenger().forwardAll(carrier, b.toByteArray()); } catch (IOException e) { plugin.getLogger().warning("Could not synchronize chat mute: " + e.getMessage()); } }
    @Override public void onPluginMessageReceived(String channel, Player source, byte[] data) { if (!"BungeeCord".equals(channel)) return; try { DataInputStream o = new DataInputStream(new ByteArrayInputStream(data)); if (!SUB.equals(o.readUTF())) return; int n = o.readUnsignedShort(); if (n <= 0 || n > o.available()) return; byte[] b = new byte[n]; o.readFully(b); DataInputStream d = new DataInputStream(new ByteArrayInputStream(b)); if (d.readInt() != PROTOCOL_VERSION || !"MUTE".equals(d.readUTF())) return; String c = d.readUTF(); boolean has = d.readBoolean(); UUID id = has ? UUID.fromString(d.readUTF()) : null; boolean muted = d.readBoolean(); if ("local".equalsIgnoreCase(c)) return; Set<UUID> set = players.computeIfAbsent(c, k -> new HashSet<>()); if (id == null) { if (muted) global.add(c); else global.remove(c); } else { if (muted) set.add(id); else set.remove(id); } save(); } catch (Exception e) { plugin.getLogger().warning("Malformed or unsupported ThunderChat mute message: " + e.getMessage()); } }
    public void reload() { load(); }
    private void load() { global.clear(); localPlayers.clear(); localGlobalMuted = false; for (String channel : channels()) players.computeIfAbsent(channel, k -> new HashSet<>()).clear(); if (!"yaml".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.type", "yaml")) || !file.exists()) return; YamlConfiguration data = YamlConfiguration.loadConfiguration(file); localGlobalMuted = data.getBoolean("local.global", false); for (String id : data.getStringList("local.players")) try { localPlayers.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { } global.addAll(data.getStringList("global")); for (String channel : channels()) for (String id : data.getStringList("players." + channel)) try { players.get(channel).add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { } }
    public void save() { if (!"yaml".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.type", "yaml"))) return; YamlConfiguration data = new YamlConfiguration(); data.set("local.global", localGlobalMuted); data.set("local.players", localPlayers.stream().map(UUID::toString).toList()); data.set("global", new ArrayList<>(global)); for (String channel : channels()) data.set("players." + channel, players.getOrDefault(channel, Collections.emptySet()).stream().map(UUID::toString).toList()); try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save mutes.yml: " + e.getMessage()); } }
    public List<String> getChannels() { List<String> all = new ArrayList<>(); all.add("local"); all.addAll(channels()); return all; }
    public Set<String> getGloballyMutedChannels() { Set<String> copy = new LinkedHashSet<>(global); if (localGlobalMuted) copy.add("local"); return Collections.unmodifiableSet(copy); }
    public Map<String, Set<UUID>> getPlayerMutes() { Map<String, Set<UUID>> copy = new LinkedHashMap<>(); if (!localPlayers.isEmpty()) copy.put("local", Set.copyOf(localPlayers)); for (Map.Entry<String, Set<UUID>> entry : players.entrySet()) if (!entry.getValue().isEmpty()) copy.put(entry.getKey(), Set.copyOf(entry.getValue())); return copy; }
}
