package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Clears the visible chat for a local gamemode or a global network channel. */
public final class ClearChatCommand implements CommandExecutor {
    private static final int CLEAR_LINES = 120;
    private final ThunderChat plugin;

    public ClearChatCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isLocal(args[0])) {
            if (!hasClearPermission(player, "local")) {
                player.sendMessage(Component.text("You don't have permission to clear this chat.", NamedTextColor.RED));
                return true;
            }
            plugin.getGlobalChatManager().clearChat(Channel.LOCAL, player);
            return true;
        }

        Channel channel = parseGlobalChannel(args[0]);
        if (channel == null) {
            player.sendMessage(Component.text("Unknown chat channel. Use local, global, staff, donator, admin, or highrank.", NamedTextColor.RED));
            return true;
        }
        if (!hasClearPermission(player, channel.id())) {
            player.sendMessage(Component.text("You don't have permission to clear that chat.", NamedTextColor.RED));
            return true;
        }
        if (!plugin.getGlobalChatManager().canUse(player, channel)) {
            player.sendMessage(Component.text("You don't have access to that chat channel.", NamedTextColor.RED));
            return true;
        }

        plugin.getGlobalChatManager().clearChat(channel, player);
        return true;
    }

    private boolean hasClearPermission(Player player, String channel) {
        return player.hasPermission("thunderchat.clearchat.*")
                || player.hasPermission("thunderchat.clearchat." + channel);
    }

    private boolean isLocal(String input) {
        String value = input.toLowerCase(Locale.ROOT);
        return value.equals("local") || value.equals("gamemode") || value.equals("server");
    }

    private Channel parseGlobalChannel(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "global", "chat" -> Channel.GLOBAL;
            case "staff", "staffchat", "sc" -> Channel.STAFF;
            case "donator", "donatorchat", "dc" -> Channel.DONATOR;
            case "admin", "adminchat", "ac" -> Channel.ADMIN;
            case "highrank", "highrankchat", "hc" -> Channel.HIGHRANK;
            default -> null;
        };
    }

    public static boolean hasBypassPermission(Player player, String channel) {
        return player.hasPermission("thunderchat.bypass.clearchat.*")
                || player.hasPermission("thunderchat.bypass.clearchat." + channel);
    }

    public static void sendClear(Player target) {
        for (int i = 0; i < CLEAR_LINES; i++) {
            target.sendMessage(Component.empty());
        }
    }
}
