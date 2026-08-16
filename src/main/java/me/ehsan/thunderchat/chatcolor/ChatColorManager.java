package me.ehsan.thunderchat.chatcolor;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stores and applies the player's selected chat color. */
public final class ChatColorManager {
    private final ThunderChat plugin;
    private final Map<UUID, String> colors = new HashMap<>();
    private final File file;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public ChatColorManager(ThunderChat plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "chat-colors.yml"); load(); }
    public boolean canUse(Player player) { return player.hasPermission("thunderchat.chatcolor"); }
    public void setColor(Player player, String color) { colors.put(player.getUniqueId(), color); save(); }
    public String getColor(Player player) { return colors.getOrDefault(player.getUniqueId(), "white"); }
    public String colorize(Player player, String message) {
        if (!canUse(player)) return message;
        String color = getColor(player);
        if ("white".equalsIgnoreCase(color)) return message;
        Component component = miniMessage.deserialize("<" + color + ">" + miniMessage.escapeTags(message) + "</" + color + ">");
        return legacy.serialize(component);
    }
    public void clear(Player player) { colors.remove(player.getUniqueId()); save(); }
    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("players") == null) return;
        for (String raw : yaml.getConfigurationSection("players").getKeys(false)) try { colors.put(UUID.fromString(raw), yaml.getString("players." + raw, "white")); } catch (IllegalArgumentException ignored) { }
    }
    private synchronized void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : colors.entrySet()) yaml.set("players." + entry.getKey(), entry.getValue());
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save chat-colors.yml: " + e.getMessage()); }
    }
}
