package me.ehsan.thunderchat.spy;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Local-only command and private-message spy state. */
public final class SpyManager {
    private final ThunderChat plugin;
    private final Map<UUID, EnumSet<Section>> enabled = new HashMap<>();
    private final Set<UUID> initialized = new HashSet<>();
    private final File file;
    private YamlConfiguration data;

    public enum Section { COMMANDS, PRIVATE_MESSAGES }

    public SpyManager(ThunderChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spy.yml");
        load();
    }

    public boolean canSpy(Player player) {
        return player.hasPermission("thunderchat.command.spy") && !player.hasPermission("thunderchat.bypass.spy");
    }

    public boolean isEnabled(Player player, Section section) {
        return enabled.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Section.class)).contains(section);
    }

    public void enableAll(Player player) {
        if (!canSpy(player)) return;
        enabled.put(player.getUniqueId(), EnumSet.allOf(Section.class));
        initialized.add(player.getUniqueId());
        save();
    }

    public void disableAll(Player player) {
        enabled.remove(player.getUniqueId());
        initialized.add(player.getUniqueId());
        save();
    }

    public void toggle(Player player, Section section) {
        if (!canSpy(player)) return;
        EnumSet<Section> set = enabled.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Section.class));
        if (!set.remove(section)) set.add(section);
        initialized.add(player.getUniqueId());
        save();
    }

    public boolean isInitialized(Player player) { return initialized.contains(player.getUniqueId()); }

    public void autoEnable(Player player) {
        if (plugin.getPluginConfig().getBoolean("spy.autoenable", true)
                && canSpy(player)
                && player.hasPermission("thunderchat.spy.autoenable")
                && !isInitialized(player)) enableAll(player);
    }

    public String status(Player player) {
        return "commands=" + isEnabled(player, Section.COMMANDS)
                + ", private-messages=" + isEnabled(player, Section.PRIVATE_MESSAGES);
    }

    public void spyPrivateMessage(Player source, Player target, String message) {
        if (!plugin.getPluginConfig().getBoolean("spy.enabled", true)
                || source.hasPermission("thunderchat.bypass.spy")
                || target.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("PM", source.getName() + " -> " + target.getName(), source.getName(), message);
        sendLocal(Section.PRIVATE_MESSAGES, output);
    }

    public void spyCommand(Player source, String command) {
        if (!plugin.getPluginConfig().getBoolean("spy.enabled", true)
                || source.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("COMMAND", "", source.getName(), "/" + command);
        sendLocal(Section.COMMANDS, output);
    }

    private String format(String type, String channel, String player, String message) {
        String format = plugin.getPluginConfig().getString(
                "spy." + (type.equals("PM") ? "private-message-format" : "command-format"),
                type.equals("PM")
                        ? "&8[&7SPY&r&8] &7{player} &8---> &7{target} &8: &7{message}"
                        : "&8[&7SPY&r&8] &7{player} &8: &7{message}"
        );
        return ChatColor.translateAlternateColorCodes('&', format
                .replace("{type}", type)
                .replace("{channel}", channel)
                .replace("{player}", player)
                .replace("{target}", channel.contains(" -> ") ? channel.substring(channel.indexOf(" -> ") + 4) : "")
                .replace("{message}", message));
    }

    private void sendLocal(Section section, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEnabled(player, section) && canSpy(player)) player.sendMessage(message);
        }
    }

    private void load() {
        if (!file.exists()) { data = new YamlConfiguration(); return; }
        data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) try {
            UUID uuid = UUID.fromString(key);
            initialized.add(uuid);
            EnumSet<Section> set = EnumSet.noneOf(Section.class);
            for (String value : data.getStringList(key)) try { set.add(Section.valueOf(value)); } catch (IllegalArgumentException ignored) { }
            if (!set.isEmpty()) enabled.put(uuid, set);
        } catch (IllegalArgumentException ignored) { }
    }

    public void save() {
        data = new YamlConfiguration();
        for (UUID uuid : initialized) {
            List<String> sections = new ArrayList<>();
            for (Section section : enabled.getOrDefault(uuid, EnumSet.noneOf(Section.class))) sections.add(section.name());
            data.set(uuid.toString(), sections);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save spy settings: " + e.getMessage());
        }
    }
}
