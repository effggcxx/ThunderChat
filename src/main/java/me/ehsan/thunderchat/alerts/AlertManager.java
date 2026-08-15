package me.ehsan.thunderchat.alerts;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Sends filter alerts locally and across the Velocity/Bungee network. */
public final class AlertManager {
    private final ThunderChat plugin;

    public AlertManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public void alert(String type, Player player, String message) {
        String key = type.toLowerCase(Locale.ROOT);
        if (!plugin.getPluginConfig().getBoolean("alerts.enabled", true)
                || !plugin.getPluginConfig().getBoolean("alerts.types." + key, true)) return;

        if (!plugin.getPluginConfig().getBoolean("alerts.broadcast-network", true)) {
            sendLocal(key, player, message);
            return;
        }

        plugin.getGlobalChatManager().sendAlert(key, player, message);
    }

    public void sendLocal(String type, Player player, String message) {
        String output = format(type, plugin.getPluginConfig().getString("network.server-name", "server"),
                player.getName(), message);
        String permission = "thunderchat.alert." + type;
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.hasPermission(permission) || recipient.hasPermission("thunderchat.alert.*")) {
                recipient.sendMessage(output);
            }
        }
    }

    public String format(String type, String server, String player, String message) {
        String format = plugin.getPluginConfig().getString("alerts.format",
                "&c&l[ALERT]&r &7{type}&r &8| &7{server}&r &8| &f{player}&r &8| &7{message}");
        String resolved = format
                .replace("{type}", type.toUpperCase(Locale.ROOT))
                .replace("{server}", server)
                .replace("{player}", player)
                .replace("{message}", message);
        return ChatColor.translateAlternateColorCodes('&', resolved);
    }

    public boolean canReceive(Player player, String type) {
        return player.hasPermission("thunderchat.alert." + type)
                || player.hasPermission("thunderchat.alert.*");
    }
}
