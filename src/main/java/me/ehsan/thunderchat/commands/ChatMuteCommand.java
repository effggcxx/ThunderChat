package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatMuteCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ChatMuteCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: /chatmute <player> [duration] -> store mute (in-memory + persisted), FilterManager checks it in ChatListener
        return true;
    }
}
