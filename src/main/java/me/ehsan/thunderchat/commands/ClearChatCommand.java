package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Clears the visible chat for a local gamemode or a network-wide global channel. */
public final class ClearChatCommand implements CommandExecutor {
    private static final int CLEAR_LINES = 120;
    private final ThunderChat plugin;

    public ClearChatCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thunderchat.clearchat")) {
            sender.sendMessage(Component.text("You don't have permission to clear chat.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            clearLocal(player);
            return true;
        }

        Channel channel = parseChannel(args[0]);
        if (channel == null) {
            player.sendMessage(Component.text("Unknown chat channel. Use local, global, staff, donator, admin, or highrank.", NamedTextColor.RED));
            return true;
        }

        if (channel == Channel.GLOBAL || channel == Channel.STAFF || channel == Channel.DONATOR
                || channel == Channel.ADMIN || channel == Channel.HIGHRANK) {
            if (!plugin.getGlobalChatManager().canUse(player, channel)) {
                player.sendMessage(Component.text("You don't have access to that chat channel.", NamedTextColor.RED));
                return true;
            }
            plugin.getGlobalChatManager().clearChat(channel, player);
            return true;
        }

        clearLocal(player);
        return true;
    }

    private void clearLocal(Player source) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.hasPermission("thunderchat.bypass.clearchat")) {
                sendClear(target);
            }
        }
        source.sendMessage(Component.text("Chat cleared for this gamemode.", NamedTextColor.GREEN));
    }

    private Channel parseChannel(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "local", "gamemode", "server" -> null;
            case "global", "chat" -> Channel.GLOBAL;
            case "staff", "staffchat", "sc" -> Channel.STAFF;
            case "donator", "donatorchat", "dc" -> Channel.DONATOR;
            case "admin", "adminchat", "ac" -> Channel.ADMIN;
            case "highrank", "highrankchat", "hc" -> Channel.HIGHRANK;
            default -> null;
        };
    }

    public static void sendClear(Player target) {
        for (int i = 0; i < CLEAR_LINES; i++) {
            target.sendMessage(Component.empty());
        }
    }
}
