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
        if (!sender.hasPermission("thunderchat.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§6§lThunderChat");
            sender.sendMessage("§7Version: §f" + plugin.getPluginMeta().getVersion());
            sender.sendMessage("§7Channels: §f6 §7| Filters: §f" + (plugin.getFilterManager().isEnabled() ? "enabled" : "disabled"));
            sender.sendMessage("§7Use §f/thunderchat reload §7to reload configuration.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.loadPluginConfig();
            plugin.getMuteManager().reload();
            sender.sendMessage("§6[ThunderChat] §aConfiguration and persistent mute data reloaded.");
            return true;
        }
        sender.sendMessage("§cUsage: /thunderchat <reload|info>");
        return true;
    }
}
