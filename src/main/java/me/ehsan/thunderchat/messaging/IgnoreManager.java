package me.ehsan.thunderchat.messaging;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IgnoreManager {
    private final ThunderChat plugin;
    private final File file;
    private BukkitTask pendingSave;
    private final Map<UUID, Set<UUID>> ignores = new ConcurrentHashMap<>();

    public IgnoreManager(ThunderChat plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "ignores.yml"); load(); }

    public void load() {
        ignores.clear();
        if (plugin.getStorage().isEnabled()) {
            String value = plugin.getStorage().load("ignore", "state").join();
            if (value == null) { value = readLegacy(); if (value != null) plugin.getStorage().put("ignore", "state", value); }
            if (value != null) parse(YamlStorage.parse(value));
            return;
        }
        String legacy = readLegacy(); if (legacy != null) parse(YamlStorage.parse(legacy));
    }

    private String readLegacy() {
        if (!file.exists()) return null;
        return YamlStorage.serialize(YamlConfiguration.loadConfiguration(file));
    }

    private void parse(YamlConfiguration data) {
        if (data.getConfigurationSection("ignores") == null) return;
        for (String key : data.getConfigurationSection("ignores").getKeys(false)) try {
            UUID playerId = UUID.fromString(key);
            Set<UUID> set = data.getStringList("ignores." + key).stream().map(UUID::fromString).collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
            if (!set.isEmpty()) ignores.put(playerId, set);
        } catch (IllegalArgumentException ignored) { }
    }

    private void scheduleSave() {
        if (pendingSave != null) return;
        pendingSave = Bukkit.getScheduler().runTaskLater(plugin, () -> { pendingSave = null; save(); }, 20L);
    }

    public void save() {
        if (pendingSave != null) { pendingSave.cancel(); pendingSave = null; }
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : ignores.entrySet()) if (!entry.getValue().isEmpty()) data.set("ignores." + entry.getKey(), entry.getValue().stream().map(UUID::toString).collect(Collectors.toList()));
        if (plugin.getStorage().isEnabled()) { plugin.getStorage().put("ignore", "state", YamlStorage.serialize(data)); return; }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save ignores.yml: " + e.getMessage()); }
    }

    public boolean isIgnoring(UUID viewer, UUID target) { Set<UUID> set = ignores.get(viewer); return set != null && set.contains(target); }
    public boolean isIgnoring(Player viewer, Player target) { return isIgnoring(viewer.getUniqueId(), target.getUniqueId()); }
    public boolean toggle(UUID viewer, UUID target) { Set<UUID> set = ignores.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()); if (set.remove(target)) { if (set.isEmpty()) ignores.remove(viewer, set); scheduleSave(); return false; } set.add(target); scheduleSave(); return true; }
    public boolean toggle(Player viewer, Player target) { return toggle(viewer.getUniqueId(), target.getUniqueId()); }
    public boolean ignore(UUID viewer, UUID target) { Set<UUID> set = ignores.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()); boolean added = set.add(target); if (added) scheduleSave(); return added; }
    public boolean ignore(Player viewer, Player target) { return ignore(viewer.getUniqueId(), target.getUniqueId()); }
    public boolean unignore(UUID viewer, UUID target) { Set<UUID> set = ignores.get(viewer); if (set == null) return false; boolean removed = set.remove(target); if (set.isEmpty()) ignores.remove(viewer, set); if (removed) scheduleSave(); return removed; }
    public boolean unignore(Player viewer, Player target) { return unignore(viewer.getUniqueId(), target.getUniqueId()); }
    public Set<UUID> getIgnored(UUID viewer) { Set<UUID> set = ignores.get(viewer); return set == null ? Collections.emptySet() : Set.copyOf(set); }
}
