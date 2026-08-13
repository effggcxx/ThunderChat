package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MsgCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public MsgCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: /msg <player> <message> -> send to target + echo to sender, set reply target both ways
        return true;
    }
}
