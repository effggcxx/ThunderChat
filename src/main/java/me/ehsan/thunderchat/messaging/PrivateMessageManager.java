package me.ehsan.thunderchat.messaging;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks who each player should /reply to, and handles formatting +
 * delivering private messages between two players.
 */
public class PrivateMessageManager {

    private final ThunderChat plugin;

    // sender UUID -> UUID of the last player they exchanged a PM with
    private final Map<UUID, UUID> replyTarget = new HashMap<>();

    public PrivateMessageManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getPluginConfig().getBoolean("private-messages.enabled", true);
    }

    /**
     * Sends a private message from one player to another, updates both
     * players' reply targets, and optionally logs it to console.
     * Returns false if delivery was blocked (e.g. target is ignoring sender).
     */
    public boolean send(Player sender, Player target, String message) {
        // Target is ignoring the sender → block delivery and notify sender
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

    /** Returns the player this UUID should /reply to, or null if none set / offline. */
    public Player getReplyTarget(UUID player) {
        UUID targetId = replyTarget.get(player);
        if (targetId == null) {
            return null;
        }
        return plugin.getServer().getPlayer(targetId);
    }
}