package me.ehsan.thunderchat.alerts;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Locale;
import java.util.Map;

/** Sends filter alerts locally and across the network. */
public final class AlertManager {
    private final ThunderChat plugin;
    public AlertManager(ThunderChat plugin) { this.plugin = plugin; }
    public void alert(String type, Player player, String message) {
        String key = type.toLowerCase(Locale.ROOT);
        if (!plugin.getPluginConfig().getBoolean("alerts.enabled", true) || !plugin.getPluginConfig().getBoolean("alerts.types." + key, true)) return;
        if (!plugin.getPluginConfig().getBoolean("alerts.broadcast-network", true)) { sendLocal(key, player, message); return; }
        plugin.getGlobalChatManager().sendAlert(key, player, message);
    }
    public void sendLocal(String type, Player player, String message) {
        String output = format(type, plugin.getPluginConfig().getString("network.server-name", "server"), player.getName(), message);
        String permission = "thunderchat.alert." + type;
        for (Player recipient : Bukkit.getOnlinePlayers()) if (recipient.hasPermission(permission) || recipient.hasPermission("thunderchat.alert.*")) recipient.sendMessage(output);
    }
    public String format(String type, String server, String player, String message) {
        return plugin.getMessagesManager().legacy("formats.alert", "<red><bold>[ALERT]</bold></red> <red>{type}</red> <dark_gray>|</dark_gray> <gray>{server}</gray> <dark_gray>|</dark_gray> <white>{player}</white> <dark_gray>|</dark_gray> <gray>{message}</gray>", Map.of("type", type.toUpperCase(Locale.ROOT), "server", server, "player", player, "message", message));
    }
    public boolean canReceive(Player player, String type) { return player.hasPermission("thunderchat.alert." + type) || player.hasPermission("thunderchat.alert.*"); }
}
