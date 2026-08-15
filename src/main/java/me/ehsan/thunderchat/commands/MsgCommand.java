package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public MsgCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Only players can use this command."); return true; }
        if (!player.hasPermission("thunderchat.msg")) { player.sendMessage(ChatColor.RED + "You don't have permission to do that."); return true; }
        if (!plugin.getMessageManager().isEnabled()) { player.sendMessage(ChatColor.RED + "Private messages are currently disabled."); return true; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /" + label + " <player> <message>"); return true; }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) { player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online."); return true; }
        if (target.equals(player)) { player.sendMessage(ChatColor.RED + "You can't message yourself."); return true; }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (plugin.getMessageManager().send(player, target, message)) plugin.getSpyManager().spyPrivateMessage(player, target, message);
        return true;
    }
}
