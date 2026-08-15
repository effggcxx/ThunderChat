package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.spy.SpyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpyCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public SpyCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Only players can use this command."); return true; }
        if (!player.hasPermission("thunderchat.command.spy")) { player.sendMessage(ChatColor.RED + "You don't have permission to spy."); return true; }
        if (args.length == 0) { player.sendMessage(ChatColor.YELLOW + "Usage: /spy <on|off|status|toggle> [chat|commands|private-messages]"); return true; }
        SpyManager spy = plugin.getSpyManager();
        switch (args[0].toLowerCase()) {
            case "on" -> { spy.enableAll(player); player.sendMessage(ChatColor.GREEN + "Spy enabled for chat, commands and private messages."); }
            case "off" -> { spy.disableAll(player); player.sendMessage(ChatColor.YELLOW + "Spy disabled."); }
            case "status" -> player.sendMessage(ChatColor.GRAY + "Spy status: " + ChatColor.WHITE + spy.status(player));
            case "toggle" -> {
                if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /spy toggle <chat|commands|private-messages>"); return true; }
                SpyManager.Section section;
                try { section = SpyManager.Section.valueOf(args[1].replace('-', '_').toUpperCase()); }
                catch (IllegalArgumentException e) { player.sendMessage(ChatColor.RED + "Unknown spy section."); return true; }
                spy.toggle(player, section);
                player.sendMessage(ChatColor.GREEN + "Spy " + section.name().toLowerCase().replace('_', '-') + " is now " + (spy.isEnabled(player, section) ? "enabled" : "disabled") + ".");
            }
            default -> player.sendMessage(ChatColor.RED + "Usage: /spy <on|off|status|toggle> [chat|commands|private-messages]");
        }
        return true;
    }
}
