package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.Map;

public class MsgCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public MsgCommand(ThunderChat plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.getMessagesManager().send(sender, "errors.player-only", "<red>Only players can use this command."); return true; }
        if (!player.hasPermission("thunderchat.msg")) { plugin.getMessagesManager().send(player, "pm.no-permission", "<red>You don't have permission to do that."); return true; }
        if (!plugin.getMessageManager().isEnabled()) { plugin.getMessagesManager().send(player, "pm.disabled", "<red>Private messages are currently disabled."); return true; }
        if (args.length < 2) { plugin.getMessagesManager().send(player, "pm.usage", "<red>Usage: /{label} <player> <message>", Map.of("label", label)); return true; }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) { plugin.getMessagesManager().send(player, "pm.target-offline", "<red>Player '{player}' is not online.</red>", Map.of("player", args[0])); return true; }
        if (target.equals(player)) { plugin.getMessagesManager().send(player, "pm.self", "<red>You can't message yourself.</red>"); return true; }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (plugin.getFilterManager().shouldBlockPrivateMessage(player, message)) return true;
        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) { plugin.getAlertManager().alert("caps", player, message); message = plugin.getCapsManager().normalize(message); plugin.getCapsManager().notifyPlayer(player); }
        if (plugin.getMessageManager().send(player, target, message)) plugin.getSpyManager().spyPrivateMessage(player, target, message);
        return true;
    }
}
