package me.ehsan.thunderchat.messages;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Centralized, reloadable player-facing messages. */
public final class MessagesManager {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_.-]+)}");
    private final ThunderChat plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private File file;
    private FileConfiguration config;

    public MessagesManager(ThunderChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String path, String fallback) {
        return config.getString(path, fallback);
    }

    public Component get(String path, String fallback, Map<String, ?> placeholders) {
        String text = raw(path, fallback);
        if (placeholders != null) {
            Matcher matcher = PLACEHOLDER.matcher(text);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                Object value = placeholders.get(matcher.group(1));
                matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group(0) : String.valueOf(value)));
            }
            matcher.appendTail(result);
            text = result.toString();
        }
        return miniMessage.deserialize(text);
    }

    public Component get(String path, String fallback) { return get(path, fallback, Map.of()); }

    public void send(org.bukkit.command.CommandSender sender, String path, String fallback, Map<String, ?> placeholders) {
        sender.sendMessage(get(path, fallback, placeholders));
    }

    public void send(org.bukkit.command.CommandSender sender, String path, String fallback) {
        sender.sendMessage(get(path, fallback));
    }

    public List<String> list(String path) { return config.getStringList(path); }

    public int intValue(String path, int fallback) { return config.getInt(path, fallback); }
}
