package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Toggles visibility of individual chat channels for the player. */
public final class ChatHideCommand implements CommandExecutor {
    private final ThunderChat plugin;

    public ChatHideCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        String target = args.length == 0 ? "local" : args[0].toLowerCase(Locale.ROOT);
        GlobalChatManager manager = plugin.getGlobalChatManager();

        if (target.equals("all")) {
            if (!player.hasPermission("thunderchat.chathide.all") && !player.hasPermission("thunderchat.chathide.*")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to hide all chat channels.");
                return true;
            }

            boolean allHidden = true;
            for (Channel channel : manager.getAvailableChannels(player)) {
                if (!manager.isHidden(player, channel)) {
                    allHidden = false;
                    break;
                }
            }

            if (allHidden) {
                manager.showAll(player);
                player.sendMessage(ChatColor.GREEN + "All available chat channels are visible again.");
            } else {
                manager.hideAll(player);
                player.sendMessage(ChatColor.YELLOW + "All available chat channels are now hidden.");
            }
            return true;
        }

        Channel channel = Channel.fromId(target);
        if (channel == null) {
            player.sendMessage(ChatColor.RED + "Unknown chat channel. Use local, global, staff, donator, admin, highrank, or all.");
            return true;
        }

        if (!manager.canUse(player, channel)) {
            player.sendMessage(ChatColor.RED + "You don't have access to that chat channel.");
            return true;
        }

        String permission = "thunderchat.chathide." + channel.id();
        if (!player.hasPermission(permission) && !player.hasPermission("thunderchat.chathide.*")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to hide that chat channel.");
            return true;
        }

        boolean hiding = !manager.isHidden(player, channel);
        manager.setHidden(player, channel, hiding);
        if (hiding) {
            player.sendMessage(ChatColor.YELLOW + channel.display() + " is now hidden for you.");
        } else {
            player.sendMessage(ChatColor.GREEN + channel.display() + " is visible again.");
        }
        return true;
    }
}
