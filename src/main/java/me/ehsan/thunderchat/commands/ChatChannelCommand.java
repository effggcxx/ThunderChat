package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Handles /chat and dedicated channel toggles. */
public final class ChatChannelCommand implements CommandExecutor {
    private final ThunderChat plugin; private final Channel dedicatedChannel;
    public ChatChannelCommand(ThunderChat plugin, Channel dedicatedChannel) { this.plugin = plugin; this.dedicatedChannel = dedicatedChannel; }
    private String normalizeChannel(String value) { return switch (value.toLowerCase(Locale.ROOT)) { case "local", "gamemode", "server" -> "local"; case "global" -> "global"; case "staff", "staffchat", "sc" -> "staff"; case "donator", "donatorchat", "dc" -> "donator"; case "admin", "adminchat", "ac" -> "admin"; case "highrank", "highrankchat", "hc" -> "highrank"; default -> null; }; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.getMessagesManager().send(sender, "errors.player-only", "<red>Only players can use chat channel commands."); return true; }
        GlobalChatManager channels = plugin.getGlobalChatManager();
        if (dedicatedChannel != null) {
            if (!channels.canUse(player, dedicatedChannel)) { plugin.getMessagesManager().send(player, "errors.no-channel-permission", "<red>You don't have permission to use that chat channel."); return true; }
            channels.toggle(player, dedicatedChannel);
            plugin.getMessagesManager().send(player, "channel.toggled", "<green>Chat channel: <yellow>{channel}</yellow><green>.</green>", Map.of("channel", channels.get(player).display())); return true;
        }
        if (args.length == 0) {
            plugin.getMessagesManager().send(player, "channel.current", "<gray>Current channel: <yellow>{channel}</yellow>", Map.of("channel", channels.get(player).id()));
            plugin.getMessagesManager().send(player, "channel.available", "<gray>Available channels:");
            for (Channel channel : channels.getAvailableChannels(player)) { String hidden = channels.isHidden(player, channel) ? plugin.getMessagesManager().raw("channel.hidden-suffix", " <dark_gray>(hidden)</dark_gray>") : ""; player.sendMessage(plugin.getMessagesManager().get("channel.entry", "<gray> - <yellow>{channel}</yellow>{hidden}", Map.of("channel", channel.id(), "hidden", hidden))); }
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("clear")) { String targetChannel = args.length > 1 ? normalizeChannel(args[1]) : "local"; if (targetChannel == null) { plugin.getMessagesManager().send(player, "errors.unknown-channel-detailed", "<red>Unknown chat channel. Use local, global, staff, donator, admin, or highrank.</red>"); return true; } return new ClearChatCommand(plugin).onCommand(player, command, label, targetChannel.equals("local") ? new String[0] : new String[]{targetChannel}); }
        if (action.equals("mute") || action.equals("unmute")) {
            if (!player.hasPermission("thunderchat.admin")) { plugin.getMessagesManager().send(player, "chat.management-no-permission", "<red>You don't have permission to manage chat mutes.</red>"); return true; }
            boolean muted = action.equals("mute"); String channel = "local"; int playerIndex = -1;
            if (args.length >= 2) { String possibleChannel = normalizeChannel(args[1]); if (possibleChannel != null) { channel = possibleChannel; if (args.length >= 3) playerIndex = 2; } else playerIndex = 1; }
            if (args.length > 3 || (playerIndex >= 0 && args.length != playerIndex + 1)) { plugin.getMessagesManager().send(player, "chat.mute-usage", "<red>Usage: /chat {action} [channel] [player]</red>", Map.of("action", action)); return true; }
            String state = muted ? plugin.getMessagesManager().raw("chat.mute-state-muted", "muted") : plugin.getMessagesManager().raw("chat.mute-state-unmuted", "unmuted");
            if (playerIndex < 0) { plugin.getMuteManager().setGlobalMuted(channel, muted); plugin.getMessagesManager().send(player, "chat.muted-global", "<green>{channel} CHAT {state} globally.</green>", Map.of("channel", channel.toUpperCase(Locale.ROOT), "state", state)); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[playerIndex]); if (target == null || target.getName() == null) { plugin.getMessagesManager().send(player, "errors.player-unknown", "<red>That player is not known on this server.</red>"); return true; }
            UUID uuid = target.getUniqueId(); plugin.getMuteManager().setPlayerMuted(channel, uuid, muted); plugin.getMessagesManager().send(player, "chat.muted-player", "<green>{player} has been {state} in {channel} CHAT.</green>", Map.of("player", target.getName(), "state", state, "channel", channel.toUpperCase(Locale.ROOT))); return true;
        }
        plugin.getMessagesManager().send(player, "chat.usage", "<red>Usage: /chat <clear|mute|unmute> [channel] [player]</red>"); return true;
    }
}
