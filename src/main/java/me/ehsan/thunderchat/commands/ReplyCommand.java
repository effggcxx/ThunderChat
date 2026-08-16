package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {

    private final ThunderChat plugin;

    public ReplyCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("thunderchat.msg")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (!plugin.getMessageManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "Private messages are currently disabled.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <message>");
            return true;
        }

        Player target = plugin.getMessageManager().getReplyTarget(player.getUniqueId());
        if (target == null) {
            player.sendMessage(ChatColor.RED + "No one to reply to.");
            return true;
        }
        if (!target.isOnline()) {
            player.sendMessage(ChatColor.RED + target.getName() + " is no longer online.");
            plugin.getMessageManager().clearReplyTarget(player.getUniqueId());
            return true;
        }

        String message = String.join(" ", args);
        if (plugin.getFilterManager().shouldBlockPrivateMessage(player, message)) return true;
        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) {
            plugin.getAlertManager().alert("caps", player, message);
            message = plugin.getCapsManager().normalize(message);
            plugin.getCapsManager().notifyPlayer(player);
        }

        if (plugin.getMessageManager().send(player, target, message)) plugin.getSpyManager().spyPrivateMessage(player, target, message);
        return true;
    }
}
