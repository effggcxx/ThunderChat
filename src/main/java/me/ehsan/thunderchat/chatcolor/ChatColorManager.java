package me.ehsan.thunderchat.chatcolor;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Stores and applies the player's MiniMessage chat color, gradient, and styles. */
public final class ChatColorManager {
    public static final List<String> COLORS = List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white");
    public static final List<String> GRADIENTS = List.of("sunset", "ocean", "forest", "fire", "candy", "aurora", "rainbow");
    public static final List<String> STYLES = List.of("bold", "italic", "underlined", "strikethrough");

    private final ThunderChat plugin;
    private final Map<UUID, String> colors = new HashMap<>();
    private final Map<UUID, String> gradients = new HashMap<>();
    private final Map<UUID, EnumSet<Style>> styles = new HashMap<>();
    private final File file;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public enum Style { BOLD, ITALIC, UNDERLINED, STRIKETHROUGH }

    public ChatColorManager(ThunderChat plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "chat-colors.yml"); load(); }
    public boolean canUse(Player player) { return player.hasPermission("thunderchat.chatcolor"); }
    public boolean canUseColor(Player player, String color) { return player.hasPermission("thunderchat.chatcolor.color.*") || player.hasPermission("thunderchat.chatcolor.color." + color); }
    public boolean canUseGradient(Player player, String gradient) { return player.hasPermission("thunderchat.chatcolor.gradient.*") || player.hasPermission("thunderchat.chatcolor.gradient." + gradient); }
    public boolean canUseStyle(Player player, String style) { return player.hasPermission("thunderchat.chatcolor.style.*") || player.hasPermission("thunderchat.chatcolor.style." + style); }

    public void setColor(Player player, String color) { colors.put(player.getUniqueId(), color); gradients.remove(player.getUniqueId()); save(); }
    public String getColor(Player player) { return colors.get(player.getUniqueId()); }
    public void setGradient(Player player, String gradient) { gradients.put(player.getUniqueId(), gradient); colors.remove(player.getUniqueId()); save(); }
    public String getGradient(Player player) { return gradients.get(player.getUniqueId()); }
    public boolean hasStyle(Player player, String style) { try { return styles.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Style.class)).contains(Style.valueOf(style.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return false; } }
    public void toggleStyle(Player player, String style) { try { Style value = Style.valueOf(style.toUpperCase(Locale.ROOT)); EnumSet<Style> selected = styles.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Style.class)); if (selected.contains(value)) selected.remove(value); else selected.add(value); if (selected.isEmpty()) styles.remove(player.getUniqueId()); save(); } catch (IllegalArgumentException ignored) { } }

    public String colorize(Player player, String message) {
        if (!canUse(player)) return message;
        StringBuilder tags = new StringBuilder();
        String color = getColor(player);
        String gradient = getGradient(player);
        if (gradient != null) tags.append('<').append(gradientTag(gradient)).append('>');
        else if (color != null && !"white".equalsIgnoreCase(color)) tags.append('<').append(color).append('>');
        EnumSet<Style> selectedStyles = styles.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Style.class));
        for (Style style : selectedStyles) tags.append('<').append(styleTag(style)).append('>');
        if (tags.isEmpty()) return message;
        StringBuilder closing = new StringBuilder();
        List<Style> selected = new ArrayList<>(selectedStyles);
        for (int i = selected.size() - 1; i >= 0; i--) closing.append("</").append(styleTag(selected.get(i))).append('>');
        if (gradient != null) closing.append("rainbow".equals(gradientTag(gradient)) ? "</rainbow>" : "</gradient>");
        else if (color != null && !"white".equalsIgnoreCase(color)) closing.append("</").append(color).append('>');
        Component component = miniMessage.deserialize(tags + miniMessage.escapeTags(message) + closing);
        return legacy.serialize(component);
    }

    private String styleTag(Style style) { return switch (style) { case BOLD -> "bold"; case ITALIC -> "italic"; case UNDERLINED -> "underlined"; case STRIKETHROUGH -> "strikethrough"; }; }
    public String gradientTag(String gradient) { return switch (gradient) {
        case "sunset" -> "gradient:#ff512f:#dd2476";
        case "ocean" -> "gradient:#00c6ff:#0072ff";
        case "forest" -> "gradient:#56ab2f:#a8e063";
        case "fire" -> "gradient:#f12711:#f5af19";
        case "candy" -> "gradient:#ff9a9e:#fad0c4";
        case "aurora" -> "gradient:#00f2fe:#4facfe:#a18cd1";
        case "rainbow" -> "rainbow";
        default -> "white";
    }; }

    public void clear(Player player) { colors.remove(player.getUniqueId()); gradients.remove(player.getUniqueId()); styles.remove(player.getUniqueId()); save(); }
    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("players") == null) return;
        for (String raw : yaml.getConfigurationSection("players").getKeys(false)) try {
            UUID id = UUID.fromString(raw);
            String color = yaml.getString("players." + raw + ".color"); if (color != null) colors.put(id, color);
            String gradient = yaml.getString("players." + raw + ".gradient"); if (gradient != null) gradients.put(id, gradient);
            EnumSet<Style> selected = EnumSet.noneOf(Style.class); for (String style : yaml.getStringList("players." + raw + ".styles")) try { selected.add(Style.valueOf(style.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException ignored) { }
            if (!selected.isEmpty()) styles.put(id, selected);
        } catch (IllegalArgumentException ignored) { }
    }
    private synchronized void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        Set<UUID> ids = new HashSet<>(); ids.addAll(colors.keySet()); ids.addAll(gradients.keySet()); ids.addAll(styles.keySet());
        for (UUID id : ids) { String path = "players." + id; yaml.set(path + ".color", colors.get(id)); yaml.set(path + ".gradient", gradients.get(id)); yaml.set(path + ".styles", styles.getOrDefault(id, EnumSet.noneOf(Style.class)).stream().map(Enum::name).map(String::toLowerCase).toList()); }
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save chat-colors.yml: " + e.getMessage()); }
    }
}