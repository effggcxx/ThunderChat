package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.ChannelManager.Channel;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;

    public ChannelCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            Channel active = plugin.getChannelManager().getActiveChannel(player);
            player.sendMessage(ChatColor.GRAY + "Current channel: " + ChatColor.YELLOW + active.getId());
            player.sendMessage(ChatColor.GRAY + "Available: " + ChatColor.YELLOW + "local" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "donator" + ChatColor.GRAY + ", " + ChatColor.YELLOW + "staff");
            return true;
        }

        Channel channel = Channel.fromId(args[0]);
        if (channel == null) {
            player.sendMessage(ChatColor.RED + "Unknown channel. Use: local, donator, staff.");
            return true;
        }

        if (!plugin.getChannelManager().canUse(player, channel)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use that channel.");
            return true;
        }

        plugin.getChannelManager().setActiveChannel(player, channel);
        player.sendMessage(ChatColor.GREEN + "Chat channel set to " + ChatColor.YELLOW + channel.getId() + ChatColor.GREEN + ".");
        return true;
    }
}
