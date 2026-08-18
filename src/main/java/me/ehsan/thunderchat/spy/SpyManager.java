package me.ehsan.thunderchat.spy;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Local-only command, private-message, anvil, sign and book spy state. */
public final class SpyManager {
    private final ThunderChat plugin;
    private final Map<UUID, EnumSet<Section>> enabled = new HashMap<>();
    private final Set<UUID> initialized = new HashSet<>();
    private final File file;
    private YamlConfiguration data;

    public enum Section { COMMANDS, PRIVATE_MESSAGES, ANVILS, SIGNS, BOOKS }

    public SpyManager(ThunderChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spy.yml");
        load();
    }

    public boolean canSpy(Player player) {
        return player.hasPermission("thunderchat.command.spy");
    }

    public boolean canSpySection(Player player, Section section) {
        if (!canSpy(player)) return false;
        return player.hasPermission("thunderchat.spy.*")
                || player.hasPermission("thunderchat.spy." + section.name().toLowerCase().replace('_', '-'));
    }

    public boolean isEnabled(Player player, Section section) {
        return enabled.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Section.class)).contains(section);
    }

    public void enableAll(Player player) {
        if (!canSpy(player)) return;
        EnumSet<Section> allowed = EnumSet.noneOf(Section.class);
        for (Section section : Section.values()) if (canSpySection(player, section)) allowed.add(section);
        enabled.put(player.getUniqueId(), allowed);
        initialized.add(player.getUniqueId());
        save();
    }

    public void disableAll(Player player) {
        enabled.remove(player.getUniqueId());
        initialized.add(player.getUniqueId());
        save();
    }

    public void toggle(Player player, Section section) {
        if (!canSpySection(player, section)) return;
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
                + ", private-messages=" + isEnabled(player, Section.PRIVATE_MESSAGES)
                + ", anvils=" + isEnabled(player, Section.ANVILS)
                + ", signs=" + isEnabled(player, Section.SIGNS)
                + ", books=" + isEnabled(player, Section.BOOKS);
    }

    public void spyPrivateMessage(Player source, Player target, String message) {
        if (!plugin.getPluginConfig().getBoolean("spy.enabled", true)
                || source.hasPermission("thunderchat.bypass.spy")
                || target.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("PM", source.getName() + " -> " + target.getName(), source.getName(), message);
        sendLocal(Section.PRIVATE_MESSAGES, output, source.getUniqueId());
    }

    public void spyCommand(Player source, String command) {
        if (!plugin.getPluginConfig().getBoolean("spy.enabled", true)
                || source.hasPermission("thunderchat.bypass.spy")) return;
        String output = format("COMMAND", "", source.getName(), "/" + command);
        sendLocal(Section.COMMANDS, output, source.getUniqueId());
    }

    public void spyAnvil(Player source, String text) {
        spyInput(source, Section.ANVILS, "ANVIL", text);
    }

    public void spySign(Player source, String text) {
        spyInput(source, Section.SIGNS, "SIGN", text);
    }

    public void spyBook(Player source, String text) {
        spyInput(source, Section.BOOKS, "BOOK", text);
    }

    private void spyInput(Player source, Section section, String type, String message) {
        if (!plugin.getPluginConfig().getBoolean("spy.enabled", true)
                || source.hasPermission("thunderchat.bypass.spy")
                || !canSpySection(source, section)) return;
        String output = format(type, "", source.getName(), message);
        sendLocal(section, output, source.getUniqueId());
    }

    private String format(String type, String channel, String player, String message) {
        String format = plugin.getPluginConfig().getString(
                "spy." + switch (type) {
                    case "PM" -> "private-message-format";
                    case "COMMAND" -> "command-format";
                    case "ANVIL" -> "anvil-format";
                    case "SIGN" -> "sign-format";
                    case "BOOK" -> "book-format";
                    default -> "command-format";
                },
                "&8[&7SPY&r&8] &7{player}&r &8: &7{message}"
        );
        return ChatColor.translateAlternateColorCodes('&', format
                .replace("{type}", type)
                .replace("{channel}", channel)
                .replace("{player}", player)
                .replace("{target}", channel.contains(" -> ") ? channel.substring(channel.indexOf(" -> ") + 4) : "")
                .replace("{message}", message));
    }

    private void sendLocal(Section section, String message, UUID sourceId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(sourceId) && isEnabled(player, section) && canSpySection(player, section)) {
                player.sendMessage(message);
            }
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
