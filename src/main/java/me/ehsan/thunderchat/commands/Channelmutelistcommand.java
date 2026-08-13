package me.ehsan.thunderchat.commands;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.muting.MuteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /channelmutelist [page] — shows which channels are globally muted and
 * which players are muted in which channel, paginated with clickable
 * prev/next buttons.
 */
public class ChannelMuteListCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 8;

    private final ThunderChat plugin;

    public ChannelMuteListCommand(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thunderchat.channelmutelist")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        int requestedPage = 1;
        if (args.length >= 1) {
            try {
                requestedPage = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Page must be a number.", NamedTextColor.RED));
                return true;
            }
        }

        List<Component> entries = buildEntries(plugin.getMuteManager());

        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("No channels or players are currently muted.", NamedTextColor.GRAY));
            return true;
        }

        int totalPages = (int) Math.ceil(entries.size() / (double) PAGE_SIZE);
        int page = Math.max(1, Math.min(requestedPage, totalPages));

        sender.sendMessage(Component.text("=== Channel Mutes (page " + page + "/" + totalPages + ") ===",
                NamedTextColor.GOLD, TextDecoration.BOLD));

        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, entries.size());
        for (int i = from; i < to; i++) {
            sender.sendMessage(entries.get(i));
        }

        sender.sendMessage(buildFooter(label, page, totalPages));
        return true;
    }

    private List<Component> buildEntries(MuteManager muteManager) {
        List<Component> entries = new ArrayList<>();

        for (String channel : muteManager.getChannels()) {
            if (muteManager.getGloballyMutedChannels().contains(channel)) {
                entries.add(Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(channel, NamedTextColor.YELLOW))
                        .append(Component.text(" — globally muted", NamedTextColor.RED)));
            }
        }

        Map<String, Set<UUID>> playerMutes = muteManager.getPlayerMutes();
        for (Map.Entry<String, Set<UUID>> entry : playerMutes.entrySet()) {
            String channel = entry.getKey();
            for (UUID id : entry.getValue()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(id);
                String name = player.getName() != null ? player.getName() : id.toString();
                entries.add(Component.text("• ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" — muted in ", NamedTextColor.GRAY))
                        .append(Component.text(channel, NamedTextColor.RED)));
            }
        }

        return entries;
    }

    private Component buildFooter(String label, int page, int totalPages) {
        Component prev = page > 1
                ? Component.text("« Prev", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/" + label + " " + (page - 1)))
                : Component.text("« Prev", NamedTextColor.DARK_GRAY);

        Component next = page < totalPages
                ? Component.text("Next »", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/" + label + " " + (page + 1)))
                : Component.text("Next »", NamedTextColor.DARK_GRAY);

        return prev.append(Component.text("   ")).append(next);
    }
}