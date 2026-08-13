package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChannelCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ChannelCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: /channel <name> -> validate against ChannelManager, permission-check, switch active channel
        return true;
    }
}
