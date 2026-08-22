package me.ehsan.thunderchat.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lists online players who have permission for a specific chat channel. */
public final class ChannelListCommand implements CommandExecutor {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final String PREFIX_TOKEN = "__THUNDERCHAT_PREFIX__";
    private static final String PLAYER_TOKEN = "__THUNDERCHAT_PLAYER__";
    private static final String SERVER_TOKEN = "__THUNDERCHAT_SERVER__";

    private final ThunderChat plugin;
    private final Channel fixedChannel;

    public ChannelListCommand(ThunderChat plugin, Channel fixedChannel) {
        this.plugin = plugin;
        this.fixedChannel = fixedChannel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Channel channel = resolveChannel(command.getName(), label);
        if (channel == null) {
            plugin.getMessagesManager().send(sender, "errors.unknown-list-type",
                    "<red>Unknown list type. Use staff, highrank, admin, or donator.</red>");
            return true;
        }

        if (sender instanceof Player player && !plugin.getGlobalChatManager().canUse(player, channel)) {
            plugin.getMessagesManager().send(sender, "errors.no-list-permission",
                    "<red>You don't have permission to view the {channel} list.</red>",
                    java.util.Map.of("channel", channel.display().toLowerCase(Locale.ROOT)));
            return true;
        }

        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (!first.equals("list") && !first.equals("online") && !first.equals("l")) {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /" + label + " [list]</red>"));
                return true;
            }
        }

        List<Player> online = collectOnlineWithPermission(channel, sender);
        String configKey = "lists." + channel.id();
        String header = plugin.getPluginConfig().getString(configKey + ".header", "<dark_gray><strikethrough>------</strikethrough></dark_gray>");
        String format = plugin.getPluginConfig().getString(configKey + ".format", "{prefix}{player} <gray>({server})</gray>");
        String footer = plugin.getPluginConfig().getString(configKey + ".footer", header);
        String nobody = plugin.getPluginConfig().getString(configKey + ".nobody",
                "<gray>There is no " + channel.id() + " <light_purple>online</light_purple>.</gray>");

        sender.sendMessage(MINI_MESSAGE.deserialize(header));
        if (online.isEmpty()) {
            sender.sendMessage(MINI_MESSAGE.deserialize(nobody));
        } else {
            String serverName = plugin.getPluginConfig().getString("network.server-name", "server");
            for (Player target : online) {
                sender.sendMessage(formatPlayerLine(format, target, serverName));
            }
        }
        sender.sendMessage(MINI_MESSAGE.deserialize(footer));
        return true;
    }

    private Channel resolveChannel(String commandName, String label) {
        if (fixedChannel != null) return fixedChannel;
        String key = (label != null ? label : commandName).toLowerCase(Locale.ROOT);
        if (key.endsWith("list")) key = key.substring(0, key.length() - 4);
        if (key.equals("s") || key.equals("sc") || key.equals("staff")) return Channel.STAFF;
        if (key.equals("h") || key.equals("hc") || key.equals("highrank") || key.equals("hr")) return Channel.HIGHRANK;
        if (key.equals("a") || key.equals("ac") || key.equals("admin")) return Channel.ADMIN;
        if (key.equals("d") || key.equals("dc") || key.equals("donator") || key.equals("donor")) return Channel.DONATOR;
        return Channel.fromId(key);
    }

    private List<Player> collectOnlineWithPermission(Channel channel, CommandSender viewer) {
        GlobalChatManager manager = plugin.getGlobalChatManager();
        List<Player> result = new ArrayList<>();
        String hidePermission = "thunderchat.list.hide." + channel.id();
        boolean bypass = viewer.hasPermission("thunderchat.bypass.list.hide");
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!manager.canUse(target, channel)) continue;
            if (!bypass && target.hasPermission(hidePermission)) continue;
            result.add(target);
        }
        result.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        return result;
    }

    private Component formatPlayerLine(String format, Player player, String serverName) {
        String prefix = resolvePrefix(player);
        String line = format
                .replace("{prefix}", PREFIX_TOKEN)
                .replace("{player}", PLAYER_TOKEN)
                .replace("{server}", SERVER_TOKEN)
                .replace("%luckpermsprefix%", PREFIX_TOKEN)
                .replace("%luckperms_prefix%", PREFIX_TOKEN)
                .replace("%player%", PLAYER_TOKEN)
                .replace("%server name%", SERVER_TOKEN)
                .replace("%server_name%", SERVER_TOKEN)
                .replace("%server%", SERVER_TOKEN);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            line = PlaceholderAPI.setPlaceholders(player, line);
        }

        Component component = MINI_MESSAGE.deserialize(line)
                .replaceText(builder -> builder.matchLiteral(PREFIX_TOKEN).replacement(parsePrefix(prefix)))
                .replaceText(builder -> builder.matchLiteral(PLAYER_TOKEN).replacement(Component.text(player.getName())))
                .replaceText(builder -> builder.matchLiteral(SERVER_TOKEN).replacement(Component.text(serverName)));
        return component;
    }

    private Component parsePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Component.empty();
        // LuckPerms/PlaceholderAPI prefixes are commonly legacy '&' formatted, while
        // newer configurations may already provide MiniMessage. Support both without
        // making the list renderer fall back to legacy output.
        if (prefix.contains("<") && prefix.contains(">")) {
            return MINI_MESSAGE.deserialize(prefix);
        }
        return LEGACY_AMPERSAND.deserialize(prefix);
    }

    private String resolvePrefix(Player player) {
        String template = plugin.getPluginConfig().getString("format.prefix-placeholder", "%luckperms_prefix% ");
        if (template == null || template.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        return PlaceholderAPI.setPlaceholders(player, template);
    }
}
