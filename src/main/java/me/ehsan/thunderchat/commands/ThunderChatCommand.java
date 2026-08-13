package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ThunderChatCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ThunderChatCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadPluginConfig();
            sender.sendMessage("§6[ThunderChat] §aConfig reloaded.");
            return true;
        }
        // TODO: other admin subcommands
        return true;
    }
}