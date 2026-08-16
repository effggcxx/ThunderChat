package me.ehsan.thunderchat.messaging;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks reply targets and handles private messages. */
public class PrivateMessageManager {
    private final ThunderChat plugin;
    private final Map<UUID, UUID> replyTarget = new HashMap<>();

    public PrivateMessageManager(ThunderChat plugin) { this.plugin = plugin; }

    public boolean isEnabled() {
        return plugin.getPluginConfig().getBoolean("private-messages.enabled", true);
    }

    public boolean send(Player sender, Player target, String message) {
        if (plugin.getIgnoreManager().isIgnoring(target, sender)) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is ignoring you.");
            return false;
        }

        String toTarget = ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + sender.getName()
                + ChatColor.GRAY + " -> " + ChatColor.LIGHT_PURPLE + "me" + ChatColor.GRAY + "] "
                + ChatColor.WHITE + message;
        String toSender = ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "me"
                + ChatColor.GRAY + " -> " + ChatColor.LIGHT_PURPLE + target.getName() + ChatColor.GRAY + "] "
                + ChatColor.WHITE + message;

        target.sendMessage(toTarget);
        sender.sendMessage(toSender);
        replyTarget.put(sender.getUniqueId(), target.getUniqueId());
        replyTarget.put(target.getUniqueId(), sender.getUniqueId());

        if (plugin.getPluginConfig().getBoolean("private-messages.log-to-console", false)) {
            plugin.getLogger().info("[PM] " + sender.getName() + " -> " + target.getName() + ": " + message);
        }
        return true;
    }

    public Player getReplyTarget(UUID player) {
        UUID targetId = replyTarget.get(player);
        if (targetId == null) return null;
        return plugin.getServer().getPlayer(targetId);
    }

    public void clearReplyTarget(UUID player) { replyTarget.remove(player); }

    public void clearPlayer(UUID player) {
        replyTarget.remove(player);
        replyTarget.entrySet().removeIf(entry -> entry.getValue().equals(player));
    }
}
