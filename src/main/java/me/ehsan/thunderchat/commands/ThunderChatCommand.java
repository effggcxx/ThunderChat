package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class ThunderChatCommand implements CommandExecutor {
    private final ThunderChat plugin;
    public ThunderChatCommand(ThunderChat plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thunderchat.admin")) { plugin.getMessagesManager().send(sender, "errors.no-permission", "<red>You don't have permission to use this command."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            plugin.getMessagesManager().send(sender, "admin.title", "<gold><bold>ThunderChat</bold></gold>");
            plugin.getMessagesManager().send(sender, "admin.info-version", "<gray>Version: <white>{version}</white>", Map.of("version", plugin.getPluginMeta().getVersion()));
            plugin.getMessagesManager().send(sender, "admin.info-status", "<gray>Channels: <white>{channels}</white> <gray>| Filters: <white>{filters}</white>", Map.of("channels", 6, "filters", plugin.getFilterManager().isEnabled() ? plugin.getMessagesManager().raw("admin.filters-enabled", "enabled") : plugin.getMessagesManager().raw("admin.filters-disabled", "disabled")));
            plugin.getMessagesManager().send(sender, "admin.info-reload", "<gray>Use <white>/thunderchat reload</white> <gray>to reload configuration.");
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) { showHelp(sender, args.length > 1 ? parsePage(args[1]) : 1); return true; }
        if (args[0].equalsIgnoreCase("reload")) { plugin.loadPluginConfig(); plugin.getMuteManager().reload(); plugin.getMessagesManager().send(sender, "admin.reload-success", "<green>Configuration and persistent mute data reloaded."); return true; }
        plugin.getMessagesManager().send(sender, "admin.usage", "<red>Usage: /thunderchat <reload|info|help> [page]");
        return true;
    }

    private int parsePage(String value) { try { return Math.max(1, Integer.parseInt(value)); } catch (NumberFormatException ignored) { return 1; } }

    private void showHelp(CommandSender sender, int requestedPage) {
        int max = Math.max(1, plugin.getMessagesManager().intValue("help.max-page", 1));
        int page = Math.min(requestedPage, max);
        plugin.getMessagesManager().send(sender, "help.title", "<gold><bold>ThunderChat Help</bold></gold> <dark_gray>({page}/{max_page})</dark_gray>", Map.of("page", page, "max_page", max));
        String key = "help.page-" + page;
        for (String line : plugin.getMessagesManager().list(key)) sender.sendMessage(Component.text(line).replaceText(builder -> builder.match("<[^>]+>").replacement(match -> Component.text(match.content()))));
    }
}
