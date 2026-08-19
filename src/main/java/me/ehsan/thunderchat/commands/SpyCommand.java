package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.spy.SpyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;

public final class SpyCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public SpyCommand(ThunderChat plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { plugin.getMessagesManager().send(sender, "errors.player-only", "<red>Only players can use this command."); return true; }
        if (!player.hasPermission("thunderchat.command.spy")) { plugin.getMessagesManager().send(player, "spy.no-permission", "<red>You don't have permission to spy."); return true; }
        if (args.length == 0) { plugin.getMessagesManager().send(player, "spy.usage", "<red>Usage: /spy <on|off|status|toggle> [commands|private-messages|anvils|signs|books]"); return true; }
        SpyManager spy = plugin.getSpyManager();
        switch (args[0].toLowerCase()) {
            case "on" -> { spy.enableAll(player); plugin.getMessagesManager().send(player, "spy.enabled-all", "<green>Spy enabled for all available sections."); }
            case "off" -> { spy.disableAll(player); plugin.getMessagesManager().send(player, "spy.disabled", "<yellow>Spy disabled."); }
            case "status" -> plugin.getMessagesManager().send(player, "spy.status", "<gray>Spy status: <white>{status}", Map.of("status", spy.status(player)));
            case "toggle" -> {
                if (args.length < 2) { plugin.getMessagesManager().send(player, "spy.toggle-usage", "<red>Usage: /spy toggle <commands|private-messages|anvils|signs|books>"); return true; }
                SpyManager.Section section;
                try { section = SpyManager.Section.valueOf(args[1].replace('-', '_').toUpperCase()); }
                catch (IllegalArgumentException e) { plugin.getMessagesManager().send(player, "spy.unknown-section", "<red>Unknown spy section."); return true; }
                if (!spy.canSpySection(player, section)) { plugin.getMessagesManager().send(player, "spy.section-no-permission", "<red>You don't have permission to spy on that input type."); return true; }
                spy.toggle(player, section);
                plugin.getMessagesManager().send(player, "spy.toggled", "<green>Spy {section} is now {state}.", Map.of("section", section.name().toLowerCase().replace('_', '-'), "state", spy.isEnabled(player, section) ? "enabled" : "disabled"));
            }
            default -> plugin.getMessagesManager().send(player, "spy.usage", "<red>Usage: /spy <on|off|status|toggle> [commands|private-messages|anvils|signs|books]");
        }
        return true;
    }
}
