package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class IgnoreCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public IgnoreCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: /ignore <player> -> toggle in a persisted ignore list, block their PMs + optionally their public chat
        return true;
    }
}
