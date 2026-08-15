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

/** Handles /chat and the dedicated /staffchat, /donatorchat, /adminchat and /highrankchat toggles. */
public final class ChatChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;
    private final Channel dedicatedChannel;

    public ChatChannelCommand(ThunderChat plugin, Channel dedicatedChannel) {
        this.plugin = plugin;
        this.dedicatedChannel = dedicatedChannel;
    }

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use chat channel commands.");
            return true;
        }

        GlobalChatManager channels = plugin.getGlobalChatManager();

        // Dedicated aliases toggle their channel: /sc -> staff, /sc again -> local.
        if (dedicatedChannel != null) {
            if (!channels.canUse(player, dedicatedChannel)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use that chat channel.");
                return true;
            }
            channels.toggle(player, dedicatedChannel);
            Channel active = channels.get(player);
            player.sendMessage(ChatColor.GREEN + "Chat channel: " + ChatColor.YELLOW + active.display() + ChatColor.GREEN + ".");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "Current channel: " + ChatColor.YELLOW + channels.get(player).id());
            player.sendMessage(ChatColor.GRAY + "Available channels:");
            for (Channel channel : channels.getAvailableChannels(player)) {
                String state = channels.isHidden(player, channel) ? ChatColor.DARK_GRAY + " (hidden)" : "";
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + channel.id() + state);
            }
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("clear")) {
            String targetChannel = args.length > 1 ? normalizeChannel(args[1]) : "local";
            if (targetChannel == null) {
                player.sendMessage(ChatColor.RED + "Unknown chat channel. Use local, global, staff, donator, admin, or highrank.");
                return true;
            }
            String[] clearArgs = targetChannel.equals("local") ? new String[0] : new String[]{targetChannel};
            return new ClearChatCommand(plugin).onCommand(player, command, label, clearArgs);
        }

        if (action.equals("mute") || action.equals("unmute")) {
            boolean muted = action.equals("mute");
            if (!player.hasPermission("thunderchat.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to manage chat mutes.");
                return true;
            }

            String channel = "local";
            int targetIndex = -1;
            if (args.length > 1) {
                String possibleChannel = normalizeChannel(args[1]);
                if (possibleChannel != null) {
                    channel = possibleChannel;
                    targetIndex = 2;
                } else {
                    targetIndex = 1;
                }
            }

            if (args.length > targetIndex + 1) {
                player.sendMessage(ChatColor.RED + "Usage: /chat " + action + " [channel] [player]");
                return true;
            }

            if (targetIndex == -1 || args.length == targetIndex) {
                plugin.getMuteManager().setGlobalMuted(channel, muted);
                player.sendMessage(ChatColor.GREEN + channel.toUpperCase(Locale.ROOT) + " CHAT " + (muted ? "muted" : "unmuted") + " globally.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[targetIndex]);
            if (target == null) target = Bukkit.getPlayerExact(args[targetIndex]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "That player is not cached on this server.");
                return true;
            }
            UUID uuid = target.getUniqueId();
            plugin.getMuteManager().setPlayerMuted(channel, uuid, muted);
            player.sendMessage(ChatColor.GREEN + target.getName() + " has been " + (muted ? "muted in " : "unmuted in ") + channel.toUpperCase(Locale.ROOT) + " CHAT.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /chat <clear|mute|unmute> [channel] [player]");
        return true;
    }
}
