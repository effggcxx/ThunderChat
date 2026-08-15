package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
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
            Channel active = plugin.getGlobalChatManager().get(player);
            player.sendMessage(ChatColor.GRAY + "Current channel: " + ChatColor.YELLOW + active.id());
            player.sendMessage(ChatColor.GRAY + "Available channels:");
            for (Channel channel : plugin.getGlobalChatManager().getAvailableChannels(player)) {
                String state = plugin.getGlobalChatManager().isHidden(player, channel) ? ChatColor.DARK_GRAY + " (hidden)" : "";
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + channel.id() + state);
            }
            return true;
        }

        Channel channel = Channel.fromId(args[0]);
        if (channel == null) {
            player.sendMessage(ChatColor.RED + "Unknown channel.");
            return true;
        }

        if (!plugin.getGlobalChatManager().canUse(player, channel)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use that channel.");
            return true;
        }

        plugin.getGlobalChatManager().set(player, channel);
        player.sendMessage(ChatColor.GREEN + "Chat channel set to " + ChatColor.YELLOW + channel.id() + ChatColor.GREEN + ".");
        return true;
    }
}
