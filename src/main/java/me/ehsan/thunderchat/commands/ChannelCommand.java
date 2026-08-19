package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;

public class ChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ChannelCommand(ThunderChat plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.getMessagesManager().send(sender, "errors.player-only", "<red>Only players can use this command."); return true; }
        if (args.length == 0) {
            Channel active = plugin.getGlobalChatManager().get(player);
            plugin.getMessagesManager().send(player, "channel.current", "<gray>Current channel: <yellow>{channel}</yellow>", Map.of("channel", active.id()));
            plugin.getMessagesManager().send(player, "channel.available", "<gray>Available channels:");
            for (Channel channel : plugin.getGlobalChatManager().getAvailableChannels(player)) {
                String hidden = plugin.getGlobalChatManager().isHidden(player, channel) ? plugin.getMessagesManager().raw("channel.hidden-suffix", " <dark_gray>(hidden)</dark_gray>") : "";
                player.sendMessage(plugin.getMessagesManager().get("channel.entry", "<gray> - <yellow>{channel}</yellow>{hidden}", Map.of("channel", channel.id(), "hidden", hidden)));
            }
            return true;
        }
        Channel channel = Channel.fromId(args[0]);
        if (channel == null) { plugin.getMessagesManager().send(player, "errors.unknown-channel", "<red>Unknown channel."); return true; }
        if (!plugin.getGlobalChatManager().canUse(player, channel)) { plugin.getMessagesManager().send(player, "errors.no-channel-permission", "<red>You don't have permission to use that chat channel."); return true; }
        plugin.getGlobalChatManager().set(player, channel);
        plugin.getMessagesManager().send(player, "channel.changed", "<green>Chat channel set to <yellow>{channel}</yellow><green>.</green>", Map.of("channel", channel.id()));
        return true;
    }
}
