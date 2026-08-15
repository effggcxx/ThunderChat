package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/** Handles /chat. Local chat is the default; global channels are explicit. */
public final class ChatChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;

    public ChatChannelCommand(ThunderChat plugin, Channel ignored) { this.plugin = plugin; }

    private String normalizeChannel(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "local", "gamemode", "server" -> "local";
            case "global" -> "global";
            case "staff", "staffchat", "sc" -> "staff";
            case "donator", "donatorchat", "dc" -> "donator";
            case "admin", "adminchat", "ac" -> "admin";
            case "highrank", "highrankchat", "hc" -> "highrank";
            default -> null;
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "Chat defaults to LOCAL CHAT on this gamemode.");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("clear")) {
            String targetChannel = args.length > 1 ? normalizeChannel(args[1]) : "local";
            if (targetChannel == null) {
                sender.sendMessage(ChatColor.RED + "Unknown chat channel. Use local, global, staff, donator, admin, or highrank.");
                return true;
            }
            String[] clearArgs = targetChannel.equals("local") ? new String[0] : new String[]{targetChannel};
            return new ClearChatCommand(plugin).onCommand(sender, command, label, clearArgs);
        }

        if (action.equals("mute") || action.equals("unmute")) {
            boolean muted = action.equals("mute");
            String channel = args.length > 1 ? normalizeChannel(args[1]) : "local";
            if (channel == null) {
                sender.sendMessage(ChatColor.RED + "Unknown chat channel. Use local, global, staff, donator, admin, or highrank.");
                return true;
            }
            if (!sender.hasPermission("thunderchat.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to manage chat mutes.");
                return true;
            }

            int targetIndex = args.length > 1 ? 2 : 1;
            if (args.length > targetIndex + 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /chat " + action + " [channel] [player]");
                return true;
            }

            if (args.length == targetIndex) {
                plugin.getMuteManager().setGlobalMuted(channel, muted);
                sender.sendMessage(ChatColor.GREEN + channel.toUpperCase(Locale.ROOT) + " CHAT " + (muted ? "muted" : "unmuted") + " globally.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[targetIndex]);
            if (target == null) target = Bukkit.getPlayerExact(args[targetIndex]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "That player is not cached on this server.");
                return true;
            }
            UUID uuid = target.getUniqueId();
            plugin.getMuteManager().setPlayerMuted(channel, uuid, muted);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been " + (muted ? "muted in " : "unmuted in ") + channel.toUpperCase(Locale.ROOT) + " CHAT.");
            return true;
        }

        // Explicit /chat <channel> switches the player to a network channel.
        String channelId = normalizeChannel(args[0]);
        if (channelId == null || channelId.equals("local")) {
            sender.sendMessage(ChatColor.RED + "Usage: /chat <clear|mute|unmute> [channel] [player]");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can switch chat channels.");
            return true;
        }
        Channel channel = GlobalChatManager.Channel.fromId(channelId);
        if (channel == null || !plugin.getGlobalChatManager().canUse(player, channel)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use that channel.");
            return true;
        }
        if (plugin.getGlobalChatManager().get(player) == channel) {
            plugin.getGlobalChatManager().set(player, null);
            player.sendMessage(ChatColor.GREEN + "Chat channel disabled. You are back in local chat.");
        } else {
            plugin.getGlobalChatManager().set(player, channel);
            player.sendMessage(ChatColor.GREEN + "Chat channel set to " + ChatColor.YELLOW + channel.name() + ChatColor.GREEN + ".");
        }
        return true;
    }
}
