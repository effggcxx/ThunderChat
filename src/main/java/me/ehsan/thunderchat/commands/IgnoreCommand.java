package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ignore <player>  — toggle ignore for a player
 * /unignore <player> — explicitly stop ignoring a player
 */
public class IgnoreCommand implements CommandExecutor {

    private final ThunderChat plugin;

    public IgnoreCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("thunderchat.ignore")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You can't ignore yourself.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("unignore")) {
            boolean wasIgnored = plugin.getIgnoreManager().unignore(player, target);
            if (wasIgnored) {
                player.sendMessage(ChatColor.GREEN + "You are no longer ignoring "
                        + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "You were not ignoring "
                        + target.getName() + ".");
            }
            return true;
        }

        // /ignore → toggle
        boolean nowIgnored = plugin.getIgnoreManager().toggle(player, target);
        if (nowIgnored) {
            player.sendMessage(ChatColor.GREEN + "You are now ignoring "
                    + ChatColor.YELLOW + target.getName() + ChatColor.GREEN
                    + ". You will no longer receive their private messages.");
        } else {
            player.sendMessage(ChatColor.GREEN + "You are no longer ignoring "
                    + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
        }
        return true;
    }
}