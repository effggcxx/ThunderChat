package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReplyCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ReplyCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: /reply <message> -> look up last PM partner, forward via MsgCommand logic
        return true;
    }
}
