package me.ehsan.thunderchat.chatcolor;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.storage.YamlStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Stores and applies the player's MiniMessage chat color, gradient, styles, and custom formatting. */
public final class ChatColorManager {
    public static final List<String> COLORS = List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white");
    public static final List<String> GRADIENTS = List.of("sunset", "ocean", "forest", "fire", "candy", "aurora", "rainbow");
    public static final List<String> STYLES = List.of("bold", "italic", "underlined", "strikethrough");
    private static final Pattern OBFUSCATED_TAG = Pattern.compile("<\\s*(?:obfuscated|obf)(?:\\s*[:>])", Pattern.CASE_INSENSITIVE);
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"), Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"), Map.entry('6', "gold"), Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"), Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"), Map.entry('f', "white"),
            Map.entry('k', "obfuscated"), Map.entry('l', "bold"), Map.entry('m', "strikethrough"), Map.entry('n', "underlined"),
            Map.entry('o', "italic"), Map.entry('r', "reset")
    );
    private final ThunderChat plugin;
    private final Map<UUID, String> colors = new ConcurrentHashMap<>();
    private final Map<UUID, String> gradients = new ConcurrentHashMap<>();
    private final Map<UUID, EnumSet<Style>> styles = new ConcurrentHashMap<>();
    private final Map<UUID, String> customFormats = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingCustomFormat = ConcurrentHashMap.newKeySet();
    private final File file;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public enum Style { BOLD, ITALIC, UNDERLINED, STRIKETHROUGH }

    public ChatColorManager(ThunderChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chat-colors.yml");
        load();
    }

    public boolean canUse(Player player) { return player.hasPermission("thunderchat.chatcolor"); }
    public boolean canUseColor(Player player, String color) { return canUse(player) && (player.hasPermission("thunderchat.chatcolor.color.*") || player.hasPermission("thunderchat.chatcolor.color." + color)); }
    public boolean canUseGradient(Player player, String gradient) { return canUse(player) && (player.hasPermission("thunderchat.chatcolor.gradient.*") || player.hasPermission("thunderchat.chatcolor.gradient." + gradient)); }
    public boolean canUseStyle(Player player, String style) { return canUse(player) && (player.hasPermission("thunderchat.chatcolor.style.*") || player.hasPermission("thunderchat.chatcolor.style." + style)); }
    public boolean canUseCustom(Player player) { return canUse(player) && (player.hasPermission("thunderchat.chatcolor.custom.*") || player.hasPermission("thunderchat.chatcolor.custom")); }
    public boolean isAwaitingCustomFormat(Player player) { return awaitingCustomFormat.contains(player.getUniqueId()); }
    public void beginCustomFormat(Player player) { awaitingCustomFormat.add(player.getUniqueId()); }
    public boolean consumeAwaitingCustomFormat(Player player) { return awaitingCustomFormat.remove(player.getUniqueId()); }
    public void cancelCustomFormat(Player player) { awaitingCustomFormat.remove(player.getUniqueId()); }

    public boolean isValidCustomFormat(String format) {
        if (format == null || format.isBlank() || OBFUSCATED_TAG.matcher(format).find()) return false;
        try { miniMessage.deserialize(format + "ThunderChat"); return true; }
        catch (RuntimeException ex) { return false; }
    }

    public void setCustomFormat(Player player, String format) {
        customFormats.put(player.getUniqueId(), format);
        colors.remove(player.getUniqueId());
        gradients.remove(player.getUniqueId());
        styles.remove(player.getUniqueId());
        save();
    }
    public String getCustomFormat(Player player) { return customFormats.get(player.getUniqueId()); }
    public void setColor(Player player, String color) { colors.put(player.getUniqueId(), color); gradients.remove(player.getUniqueId()); customFormats.remove(player.getUniqueId()); save(); }
    public String getColor(Player player) { return colors.get(player.getUniqueId()); }
    public void setGradient(Player player, String gradient) { gradients.put(player.getUniqueId(), gradient); colors.remove(player.getUniqueId()); customFormats.remove(player.getUniqueId()); save(); }
    public String getGradient(Player player) { return gradients.get(player.getUniqueId()); }

    public boolean hasStyle(Player player, String style) {
        try { return styles.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Style.class)).contains(Style.valueOf(style.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException e) { return false; }
    }

    public void toggleStyle(Player player, String style) {
        try {
            Style value = Style.valueOf(style.toUpperCase(Locale.ROOT));
            EnumSet<Style> selected = styles.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Style.class));
            synchronized (selected) {
                if (selected.contains(value)) selected.remove(value); else selected.add(value);
                if (selected.isEmpty()) styles.remove(player.getUniqueId(), selected);
            }
            customFormats.remove(player.getUniqueId());
            save();
        } catch (IllegalArgumentException ignored) { }
    }

    public String colorize(Player player, String message) {
        return legacy.serialize(colorizeComponent(player, message));
    }

    /** Applies the player's configured formatting without leaving the Adventure API. */
    public Component colorizeComponent(Player player, String message) {
        if (!canUse(player)) return Component.text(message);
        String custom = getCustomFormat(player);
        if (custom != null && !custom.isBlank()) return miniMessage.deserialize(custom + escapeLegacyCodes(message));

        String color = getColor(player);
        String gradient = getGradient(player);
        EnumSet<Style> selectedStyles = styles.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Style.class));
        List<Style> selected;
        synchronized (selectedStyles) { selected = new ArrayList<>(selectedStyles); }

        StringBuilder tags = new StringBuilder();
        if (gradient != null) tags.append('<').append(gradientTag(gradient)).append('>');
        else if (color != null && !"white".equalsIgnoreCase(color)) tags.append('<').append(color).append('>');
        for (Style style : selected) tags.append('<').append(styleTag(style)).append('>');
        if (tags.isEmpty()) return deserializeLegacyText(message);

        StringBuilder closing = new StringBuilder();
        for (int i = selected.size() - 1; i >= 0; i--) closing.append("</").append(styleTag(selected.get(i))).append('>');
        if (gradient != null) closing.append("rainbow".equals(gradientTag(gradient)) ? "</rainbow>" : "</gradient>");
        else if (color != null && !"white".equalsIgnoreCase(color)) closing.append("</").append(color).append('>');
        return miniMessage.deserialize(tags + escapeLegacyCodes(message) + closing);
    }

    /**
     * Converts Minecraft legacy (&a / §a) color codes to MiniMessage before parsing.
     * This keeps legacy and MiniMessage formatting consistent instead of letting & codes
     * appear literally when the MiniMessage parser happens to accept the surrounding text.
     */
    private String escapeLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder converted = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if ((current == '&' || current == '§') && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if ((next == 'x' || next == 'X') && i + 13 < input.length()) {
                    StringBuilder hex = new StringBuilder(6);
                    boolean valid = true;
                    for (int j = 0; j < 6; j++) {
                        int marker = i + 2 + j * 2;
                        if (input.charAt(marker) != '&' && input.charAt(marker) != '§') { valid = false; break; }
                        char digit = input.charAt(marker + 1);
                        if (Character.digit(digit, 16) < 0) { valid = false; break; }
                        hex.append(digit);
                    }
                    if (valid) {
                        converted.append("<#").append(hex).append('>');
                        i += 13;
                        continue;
                    }
                }
                String tag = LEGACY_TAGS.get(Character.toLowerCase(next));
                if (tag != null) {
                    converted.append('<').append(tag).append('>');
                    i++;
                    continue;
                }
            }
            converted.append(current);
        }
        return miniMessage.escapeTags(converted.toString());
    }

    private Component deserializeLegacyText(String message) {
        return miniMessage.deserialize(escapeLegacyCodes(message));
    }

    private String styleTag(Style style) { return switch (style) { case BOLD -> "bold"; case ITALIC -> "italic"; case UNDERLINED -> "underlined"; case STRIKETHROUGH -> "strikethrough"; }; }
    public String gradientTag(String gradient) { return switch (gradient) { case "sunset" -> "gradient:#ff512f:#dd2476"; case "ocean" -> "gradient:#00c6ff:#0072ff"; case "forest" -> "gradient:#56ab2f:#a8e063"; case "fire" -> "gradient:#f12711:#f5af19"; case "candy" -> "gradient:#ff9a9e:#fad0c4"; case "aurora" -> "gradient:#00f2fe:#4facfe:#a18cd1"; case "rainbow" -> "rainbow"; default -> "white"; }; }
    public void clear(Player player) { awaitingCustomFormat.remove(player.getUniqueId()); colors.remove(player.getUniqueId()); gradients.remove(player.getUniqueId()); customFormats.remove(player.getUniqueId()); styles.remove(player.getUniqueId()); save(); }
    public void clearRuntime(UUID id) { awaitingCustomFormat.remove(id); }

    private void load() {
        colors.clear(); gradients.clear(); customFormats.clear(); styles.clear();
        String serialized = plugin.getStorage().isEnabled() ? plugin.getStorage().load("chatcolor", "state").join() : null;
        if (serialized == null && file.exists()) {
            try { serialized = YamlConfiguration.loadConfiguration(file).saveToString(); } catch (Exception ignored) { }
            if (serialized != null && plugin.getStorage().isEnabled()) plugin.getStorage().put("chatcolor", "state", serialized);
        }
        if (serialized == null) return;
        YamlConfiguration yaml = YamlStorage.parse(serialized);
        if (yaml.getConfigurationSection("players") == null) return;
        for (String raw : yaml.getConfigurationSection("players").getKeys(false)) try {
            UUID id = UUID.fromString(raw);
            String color = yaml.getString("players." + raw + ".color"); if (color != null) colors.put(id, color);
            String gradient = yaml.getString("players." + raw + ".gradient"); if (gradient != null) gradients.put(id, gradient);
            String custom = yaml.getString("players." + raw + ".custom"); if (custom != null) customFormats.put(id, custom);
            EnumSet<Style> selected = EnumSet.noneOf(Style.class);
            for (String style : yaml.getStringList("players." + raw + ".styles")) try { selected.add(Style.valueOf(style.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException ignored) { }
            if (!selected.isEmpty()) styles.put(id, selected);
        } catch (IllegalArgumentException ignored) { }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        Set<UUID> ids = new HashSet<>(); ids.addAll(colors.keySet()); ids.addAll(gradients.keySet()); ids.addAll(customFormats.keySet()); ids.addAll(styles.keySet());
        for (UUID id : ids) {
            String path = "players." + id;
            yaml.set(path + ".color", colors.get(id)); yaml.set(path + ".gradient", gradients.get(id)); yaml.set(path + ".custom", customFormats.get(id));
            EnumSet<Style> selected = styles.getOrDefault(id, EnumSet.noneOf(Style.class));
            synchronized (selected) { yaml.set(path + ".styles", selected.stream().map(Enum::name).map(String::toLowerCase).toList()); }
        }
        if (plugin.getStorage().isEnabled()) { plugin.getStorage().put("chatcolor", "state", YamlStorage.serialize(yaml)); return; }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Could not save chat-colors.yml: " + e.getMessage()); }
    }
}
