package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;

public class ReplyCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ReplyCommand(ThunderChat plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.getMessagesManager().send(sender, "errors.player-only", "<red>Only players can use this command."); return true; }
        if (!player.hasPermission("thunderchat.msg")) { plugin.getMessagesManager().send(player, "pm.no-permission", "<red>You don't have permission to do that."); return true; }
        if (!plugin.getMessageManager().isEnabled()) { plugin.getMessagesManager().send(player, "pm.disabled", "<red>Private messages are currently disabled."); return true; }
        if (args.length < 1) { plugin.getMessagesManager().send(player, "pm.reply-usage", "<red>Usage: /{label} <message>", Map.of("label", label)); return true; }
        Player target = plugin.getMessageManager().getReplyTarget(player.getUniqueId());
        if (target == null) { plugin.getMessagesManager().send(player, "pm.no-reply", "<red>No one to reply to.</red>"); return true; }
        if (!target.isOnline()) { plugin.getMessagesManager().send(player, "pm.target-left", "<red>{player} is no longer online.</red>", Map.of("player", target.getName())); plugin.getMessageManager().clearReplyTarget(player.getUniqueId()); return true; }
        String message = String.join(" ", args);
        if (plugin.getFilterManager().shouldBlockPrivateMessage(player, message)) return true;
        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) { plugin.getAlertManager().alert("caps", player, message); message = plugin.getCapsManager().normalize(message); plugin.getCapsManager().notifyPlayer(player); }
        if (plugin.getMessageManager().send(player, target, message)) plugin.getSpyManager().spyPrivateMessage(player, target, message);
        return true;
    }
}
