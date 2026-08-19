package me.ehsan.thunderchat.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Unknown list type. Use staff, highrank, admin, or donator.");
            return true;
        }
        if (sender instanceof Player player && !plugin.getGlobalChatManager().canUse(player, channel)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view the " + channel.display().toLowerCase(Locale.ROOT) + " list.");
            return true;
        }
        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (!first.equals("list") && !first.equals("online") && !first.equals("l")) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [list]");
                return true;
            }
        }

        List<Player> online = collectOnlineWithPermission(channel, sender);
        String configKey = "lists." + channel.id();
        String header = plugin.getPluginConfig().getString(configKey + ".header", "&8&m------&5&m------&d&m------&5&m------&8&m------&f");
        String format = plugin.getPluginConfig().getString(configKey + ".format", "%luckperms_prefix%%player% &7(&d%server_name%&7) ");
        String footer = plugin.getPluginConfig().getString(configKey + ".footer", "&8&m------&5&m------&d&m------&5&m------&8&m------&f");
        String nobody = plugin.getPluginConfig().getString(configKey + ".nobody", " &7There is no " + channel.id() + " &donline&7.");

        sender.sendMessage(colorize(header));
        if (online.isEmpty()) sender.sendMessage(colorize(nobody));
        else {
            String serverName = plugin.getPluginConfig().getString("network.server-name", "server");
            for (Player target : online) sender.sendMessage(formatPlayerLine(format, target, serverName));
        }
        sender.sendMessage(colorize(footer));
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

    private String formatPlayerLine(String format, Player player, String serverName) {
        String prefix = resolvePrefix(player);
        String line = format.replace("%luckpermsprefix%", prefix).replace("%luckperms_prefix%", prefix)
                .replace("%player%", player.getName()).replace("%server name%", serverName)
                .replace("%server_name%", serverName).replace("%server%", serverName)
                .replace("{prefix}", prefix).replace("{player}", player.getName()).replace("{server}", serverName);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) line = PlaceholderAPI.setPlaceholders(player, line);
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    private String resolvePrefix(Player player) {
        String template = plugin.getPluginConfig().getString("format.prefix-placeholder", "%luckperms_prefix% ");
        if (template == null || template.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        return PlaceholderAPI.setPlaceholders(player, template);
    }

    private String colorize(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }
}
