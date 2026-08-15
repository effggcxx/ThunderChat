package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Locale;
import java.util.UUID;

/** Handles /chat and /globalchat mute management for every supported network channel. */
public class ChatMuteCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ChatMuteCommand(ThunderChat plugin) { this.plugin = plugin; }

    private String normalizeChannel(String value) {
        String channel = value.toLowerCase(Locale.ROOT);
        return switch (channel) {
            case "global", "staff", "donator", "admin", "highrank" -> channel;
            default -> null;
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || (!args[0].equalsIgnoreCase("mute") && !args[0].equalsIgnoreCase("unmute"))) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <mute|unmute> [channel] [player]");
            return true;
        }

        if (!sender.hasPermission("thunderchat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage chat mutes.");
            return true;
        }

        boolean muted = args[0].equalsIgnoreCase("mute");
        String channel = "global";
        int targetIndex = 1;

        if (args.length > 1) {
            String possibleChannel = normalizeChannel(args[1]);
            if (possibleChannel != null) {
                channel = possibleChannel;
                targetIndex = 2;
            }
        }

        if (args.length > targetIndex + 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <mute|unmute> [channel] [player]");
            return true;
        }

        if (args.length == targetIndex) {
            plugin.getMuteManager().setGlobalMuted(channel, muted);
            sender.sendMessage(ChatColor.GREEN + channel.toUpperCase(Locale.ROOT) + " CHAT " + (muted ? "muted" : "unmuted") + " globally.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[targetIndex]);
        if (target == null) {
            target = Bukkit.getPlayerExact(args[targetIndex]);
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "That player is not cached on this server.");
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getMuteManager().setPlayerMuted(channel, uuid, muted);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " has been " + (muted ? "muted in " : "unmuted in ") + channel.toUpperCase(Locale.ROOT) + " CHAT.");
        return true;
    }
}
