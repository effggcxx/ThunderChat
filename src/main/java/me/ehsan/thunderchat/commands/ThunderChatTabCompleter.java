package me.ehsan.thunderchat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Shared lightweight tab completion for ThunderChat commands. */
public final class ThunderChatTabCompleter implements TabCompleter {
    private static final List<String> CHANNELS = List.of("local", "global", "staff", "donator", "admin", "highrank");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("channel")) return filter(args, CHANNELS);
        if (name.equals("chathide")) {
            List<String> values = new ArrayList<>(CHANNELS); values.add("all"); return filter(args, values);
        }
        if (name.equals("chat")) {
            if (args.length <= 1) return filter(args, List.of("clear", "mute", "unmute"));
            if (args.length == 2) return filter(args, CHANNELS);
            if (args.length >= 3) return playerNames(args[args.length - 1]);
        }
        if (name.equals("clearchat")) return filter(args, CHANNELS);
        if (name.equals("channelmutelist")) return args.length == 1 ? List.of("1", "2", "3") : Collections.emptyList();
        if (name.equals("spy")) {
            if (args.length <= 1) return filter(args, List.of("on", "off", "status", "toggle"));
            if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                return filter(args, List.of("commands", "private-messages", "anvils", "signs", "books"));
            }
        }
        if (name.equals("thunderchat")) return filter(args, List.of("reload", "info"));
        if (name.equals("ignore") || name.equals("unignore")) return playerNames(args.length == 0 ? "" : args[args.length - 1]);
        if (name.equals("msg")) return args.length <= 1 ? playerNames(args.length == 0 ? "" : args[0]) : Collections.emptyList();
        return Collections.emptyList();
    }

    private List<String> playerNames(String prefix) {
        String value = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(p -> p.getName())
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(value)).sorted().collect(Collectors.toList());
    }

    private List<String> filter(String[] args, List<String> values) {
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().collect(Collectors.toList());
    }
}
