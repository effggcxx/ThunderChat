package me.ehsan.thunderchat.channels;

import com.google.gson.JsonParseException;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.commands.ClearChatCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Channel routing/state and network chat delivery. */
public final class GlobalChatManager {
    private static final int PROTOCOL_VERSION = 2;
    private enum PacketKind { CHAT, CLEAR, ALERT }

    public static final class Channel {
        private static final Map<String, Channel> REGISTRY = new ConcurrentHashMap<>();
        public static final Channel LOCAL = registerBuiltin("local", "LOCAL CHAT", false);
        public static final Channel GLOBAL = registerBuiltin("global", "GLOBAL CHAT", true);
        public static final Channel DONATOR = registerBuiltin("donator", "DONATOR CHAT", true);
        public static final Channel STAFF = registerBuiltin("staff", "STAFF CHAT", true);
        public static final Channel ADMIN = registerBuiltin("admin", "ADMIN CHAT", true);
        public static final Channel HIGHRANK = registerBuiltin("highrank", "HIGH RANK CHAT", true);
        private final String id;
        private volatile String display;
        private volatile boolean network;
        private volatile String permission;

        private Channel(String id, String display, boolean network, String permission) {
            this.id = id; this.display = display; this.network = network; this.permission = permission;
        }
        private static Channel registerBuiltin(String id, String display, boolean network) {
            Channel channel = new Channel(id, display, network, "thunderchat.channel." + id);
            REGISTRY.put(id, channel); return channel;
        }
        private static Channel registerConfigured(String id, String display, boolean network, String permission) {
            Channel existing = REGISTRY.get(id.toLowerCase(Locale.ROOT));
            if (existing != null) { existing.display = display; existing.network = network; existing.permission = permission; return existing; }
            Channel channel = new Channel(id.toLowerCase(Locale.ROOT), display, network, permission);
            REGISTRY.put(channel.id, channel); return channel;
        }
        public String id() { return id; }
        public String display() { return display; }
        public boolean isNetwork() { return network; }
        public String permission() { return permission; }
        public static Channel fromId(String id) { return id == null ? null : REGISTRY.get(id.toLowerCase(Locale.ROOT)); }
        public static Channel[] values() { return REGISTRY.values().toArray(new Channel[0]); }
        public static List<Channel> all() { return List.copyOf(REGISTRY.values()); }
    }

    private static GlobalChatManager instance;
    private final ThunderChat plugin;
    private final Map<UUID, Channel> active = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Channel>> hidden = new ConcurrentHashMap<>();
    private final File stateFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();
    /** Serialized state snapshot. Mutated only on the server thread and persisted asynchronously. */
    private volatile String stateSnapshot;

    public GlobalChatManager(ThunderChat plugin) {
        this.plugin = plugin; instance = this;
        stateFile = new File(plugin.getDataFolder(), "channel-state.yml");
        registerConfiguredChannels(); loadState();
    }
    public static GlobalChatManager getInstance() { return instance; }

    private void registerConfiguredChannels() {
        ConfigurationSection section = plugin.getPluginConfig().getConfigurationSection("channels");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            String normalized = id.toLowerCase(Locale.ROOT);
            String display = plugin.getPluginConfig().getString("channels." + id + ".display",
                    normalized.equals("local") ? "LOCAL CHAT" : normalized.toUpperCase(Locale.ROOT) + " CHAT");
            boolean network = !normalized.equals("local") && plugin.getPluginConfig().getBoolean("channels." + id + ".network", true);
            String permission = plugin.getPluginConfig().getString("channels." + id + ".permission", "thunderchat.channel." + normalized);
            Channel.registerConfigured(normalized, display, network, permission);
        }
    }
    public Channel get(Player player) { return active.getOrDefault(player.getUniqueId(), Channel.LOCAL); }
    public void set(Player player, Channel channel) { active.put(player.getUniqueId(), channel == null ? Channel.LOCAL : channel); savePlayerState(player.getUniqueId()); }
    public void toggle(Player player, Channel channel) { set(player, get(player) == channel ? Channel.LOCAL : channel); }
    public boolean canUse(Player player, Channel channel) { return channel == Channel.LOCAL || player.hasPermission(channel.permission()); }
    public boolean isHidden(Player player, Channel channel) { Set<Channel> channels = hidden.get(player.getUniqueId()); return channels != null && channels.contains(channel); }

    public void setHidden(Player player, Channel channel, boolean value) {
        Set<Channel> channels = hidden.computeIfAbsent(player.getUniqueId(), key -> ConcurrentHashMap.newKeySet());
        if (value) channels.add(channel); else channels.remove(channel);
        if (get(player) == channel && value) active.put(player.getUniqueId(), Channel.LOCAL);
        if (channels.isEmpty()) hidden.remove(player.getUniqueId(), channels);
        savePlayerState(player.getUniqueId());
    }
    public void toggleHidden(Player player, Channel channel) { setHidden(player, channel, !isHidden(player, channel)); }
    public void hideAll(Player player) {
        Set<Channel> channels = ConcurrentHashMap.newKeySet(); channels.addAll(Arrays.asList(Channel.values()));
        hidden.put(player.getUniqueId(), channels); active.put(player.getUniqueId(), Channel.LOCAL); savePlayerState(player.getUniqueId());
    }
    public void showAll(Player player) { hidden.remove(player.getUniqueId()); savePlayerState(player.getUniqueId()); }
    public List<Channel> getAvailableChannels(Player player) {
        List<Channel> result = new ArrayList<>(); for (Channel channel : Channel.values()) if (canUse(player, channel)) result.add(channel);
        result.sort((a,b) -> a.id().compareToIgnoreCase(b.id())); return result;
    }
    public void clearPlayer(UUID id) { active.remove(id); hidden.remove(id); }
    public void restorePlayer(Player player) {
        UUID id = player.getUniqueId(); Channel channel = active.getOrDefault(id, Channel.LOCAL);
        if (!canUse(player, channel) || isHidden(player, channel)) active.put(id, Channel.LOCAL);
    }

    private void loadState() {
        active.clear(); hidden.clear();
        String serialized = plugin.getStorage().load("channel", "state").join();
        if (serialized == null && stateFile.exists()) {
            serialized = stateFileContent();
            if (serialized != null) plugin.getStorage().put("channel", "state", serialized);
        }
        stateSnapshot = serialized;
        if (serialized == null) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new StringReader(serialized));
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        for (String rawId : players.getKeys(false)) {
            try {
                UUID id = UUID.fromString(rawId);
                Channel channel = Channel.fromId(data.getString("players." + rawId + ".active", "local"));
                active.put(id, channel == null ? Channel.LOCAL : channel);
                Set<Channel> channels = ConcurrentHashMap.newKeySet();
                for (String hiddenId : data.getStringList("players." + rawId + ".hidden")) {
                    Channel hiddenChannel = Channel.fromId(hiddenId); if (hiddenChannel != null) channels.add(hiddenChannel);
                }
                if (!channels.isEmpty()) hidden.put(id, channels);
            } catch (IllegalArgumentException ignored) { }
        }
    }
    private String stateFileContent() {
        try { return stateFile.exists() ? YamlConfiguration.loadConfiguration(stateFile).saveToString() : null; }
        catch (Exception ignored) { return null; }
    }

    /** Updates the cached snapshot and queues persistence without performing any DB read. */
    private void savePlayerState(UUID id) {
        YamlConfiguration data = stateSnapshot == null ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(new StringReader(stateSnapshot));
        String path = "players." + id;
        data.set(path + ".active", active.getOrDefault(id, Channel.LOCAL).id());
        Set<Channel> channels = hidden.get(id);
        data.set(path + ".hidden", channels == null ? Collections.emptyList() : channels.stream().map(Channel::id).toList());
        stateSnapshot = data.saveToString();
        plugin.getStorage().put("channel", "state", stateSnapshot);
    }

    public void send(Player player, String text) {
        Channel channel = get(player);
        if (!canUse(player, channel) || isHidden(player, channel)) { set(player, Channel.LOCAL); channel = Channel.LOCAL; }
        if (plugin.getMuteManager().isMuted(player, channel.id())) {
            plugin.getMessagesManager().send(player, "chat.muted", "<red>That chat is currently muted for you."); return;
        }
        String server = plugin.getPluginConfig().getString("network.server-name", "server");
        Component message = plugin.getChatColorManager().colorizeComponent(player, text);
        message = applyMentionHighlight(message, player.hasPermission("thunderchat.mention"));
        Component output = format(getFormat(channel), channel, server, prefix(player), player.getName(), message, player);
        output = plugin.getInteractiveChatManager().decorate(player, output);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (shouldReceive(recipient, player.getUniqueId(), channel)) { recipient.sendMessage(output); playMentionSoundIfNeeded(recipient, text, player.hasPermission("thunderchat.mention")); }
        }
        if (channel.isNetwork()) forwardChat(player, channel, player.getUniqueId(), player.getName(), text, output, player.hasPermission("thunderchat.mention"));
    }

    private boolean shouldReceive(Player recipient, UUID senderId, Channel channel) {
        if (isHidden(recipient, channel)) return false;
        if (channel != Channel.LOCAL && !canUse(recipient, channel)) return false;
        return !plugin.getPluginConfig().getBoolean("ignore.public-chat.enabled", false) || !plugin.getIgnoreManager().isIgnoring(recipient.getUniqueId(), senderId);
    }
    private Component applyMentionHighlight(Component message, boolean allowed) { return !allowed || !plugin.getPluginConfig().getBoolean("mentions.enabled", true) ? message : highlightMentions(message); }
    private Component highlightMentions(Component message) {
        String configured = plugin.getPluginConfig().getString("mentions.highlight-color", "<yellow>");
        String miniColor = configured.startsWith("&") ? "<yellow>" : configured;
        for (Player target : Bukkit.getOnlinePlayers()) {
            String name = target.getName();
            Component replacement;
            try { replacement = miniMessage.deserialize(miniColor + "@" + name + "<reset>"); } catch (RuntimeException ignored) { replacement = Component.text("@" + name); }
            message = message.replaceText(TextReplacementConfig.builder().match("(?i)(?<![A-Za-z0-9_])@" + Pattern.quote(name) + "\\b").replacement(replacement).build());
        }
        return message;
    }
    private void playMentionSoundIfNeeded(Player recipient, String message, boolean allowed) {
        if (!allowed || !plugin.getPluginConfig().getBoolean("mentions.enabled", true)) return;
        if (!message.matches("(?s).*?(?i)(?<![A-Za-z0-9_])@" + Pattern.quote(recipient.getName()) + "\\b.*")) return;
        try { Sound sound = resolveSound(plugin.getPluginConfig().getString("mentions.sound", "ENTITY_EXPERIENCE_ORB_PICKUP")); if (sound != null) recipient.playSound(recipient.getLocation(), sound, 1.0f, 1.0f); } catch (IllegalArgumentException ignored) { }
    }

    public void sendAlert(String type, Player source, String blockedMessage) {
        String output = plugin.getAlertManager().format(type, plugin.getPluginConfig().getString("network.server-name", "server"), source.getName(), blockedMessage);
        sendAlertLocally(type, output); if (!plugin.getPluginConfig().getBoolean("alerts.broadcast-network", true)) return;
        try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.ALERT.name()); data.writeUTF(type); data.writeUTF(source.getUniqueId().toString()); data.writeUTF(output); data.flush(); plugin.getNetworkMessenger().forwardAll(source, bytes.toByteArray()); }
        catch (IOException e) { plugin.getLogger().warning("Could not forward filter alert: " + e.getMessage()); }
    }
    private void sendAlertLocally(String type, String output) { for (Player recipient : Bukkit.getOnlinePlayers()) if (plugin.getAlertManager().canReceive(recipient, type)) recipient.sendMessage(output); }

    public void clearChat(Channel channel, Player source) {
        if (channel == Channel.LOCAL) {
            for (Player recipient : Bukkit.getOnlinePlayers()) if (!isHidden(recipient, Channel.LOCAL) && !ClearChatCommand.hasBypassPermission(recipient, "local")) ClearChatCommand.sendClear(recipient);
            plugin.getMessagesManager().send(source, "chat.cleared-local", "<green>Chat cleared for this gamemode."); return;
        }
        for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, channel) && !isHidden(recipient, channel) && !ClearChatCommand.hasBypassPermission(recipient, channel.id())) ClearChatCommand.sendClear(recipient);
        forwardClear(source, channel); plugin.getMessagesManager().send(source, "chat.cleared-channel", "<green>Cleared <yellow>{channel}</yellow>.", Map.of("channel", channel.id()));
    }
    private String getFormat(Channel channel) {
        String configured = plugin.getPluginConfig().getString("format.channels." + channel.id());
        if (configured != null && !configured.isEmpty()) return configured;
        if (channel == Channel.LOCAL) return plugin.getPluginConfig().getString("format.normal", "{prefix}<reset>{player}<gray>: <white>{message}");
        return plugin.getPluginConfig().getString("format.global", "<gray>[{channel}] <gray>[{server}] {prefix}<reset>{player}<gray>: <white>{message}");
    }
    private Component format(String format, Channel channel, String server, String prefix, String player, Component message, Player placeholderPlayer) {
        String resolved = format.replace("{channel}", channel.display()).replace("{server}", server).replace("{player}", player);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) resolved = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(placeholderPlayer, resolved);
        TagResolver resolver = TagResolver.builder().resolver(StandardTags.color()).resolver(StandardTags.decorations()).resolver(StandardTags.gradient())
                .resolver(TagResolver.resolver("tc_prefix", net.kyori.adventure.text.minimessage.tag.Tag.inserting(legacy.deserialize(prefix))))
                .resolver(TagResolver.resolver("tc_message", net.kyori.adventure.text.minimessage.tag.Tag.inserting(message))).build();
        String miniFormat = resolved.replace("{prefix}", "<tc_prefix>").replace("{message}", "<tc_message>");
        try { return miniMessage.deserialize(miniFormat, resolver); } catch (RuntimeException ignored) { return legacy.deserialize(resolved.replace("{prefix}", prefix)); }
    }
    private String prefix(Player player) {
        String template = plugin.getPluginConfig().getString("format.prefix-placeholder", "%luckperms_prefix% ");
        if (template == null || template.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, template);
    }
    private void forwardChat(Player player, Channel channel, UUID senderId, String senderName, String rawMessage, Component resolvedOutput, boolean mentionsAllowed) {
        try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.CHAT.name()); data.writeUTF(channel.id()); data.writeUTF(senderId.toString()); data.writeUTF(senderName); data.writeBoolean(mentionsAllowed); data.writeUTF(rawMessage); data.writeUTF(gson.serialize(resolvedOutput)); data.flush(); plugin.getNetworkMessenger().forwardAll(player, bytes.toByteArray()); }
        catch (IOException e) { plugin.getLogger().warning("Could not forward global chat: " + e.getMessage()); }
    }
    private void forwardClear(Player player, Channel channel) {
        try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.CLEAR.name()); data.writeUTF(channel.id()); data.flush(); plugin.getNetworkMessenger().forwardAll(player, bytes.toByteArray()); }
        catch (IOException e) { plugin.getLogger().warning("Could not forward chat clear: " + e.getMessage()); }
    }
    public void onNetworkPacket(String channel, Player source, byte[] payload) {
        if (!"BungeeCord".equals(channel) || !plugin.getPluginConfig().getBoolean("network.enabled", true)) return;
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload)); if (input.readInt() != PROTOCOL_VERSION) return;
            String kind = input.readUTF();
            if (PacketKind.CLEAR.name().equals(kind)) {
                Channel clearChannel = Channel.fromId(input.readUTF()); if (clearChannel == null || clearChannel == Channel.LOCAL) return;
                for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, clearChannel) && !isHidden(recipient, clearChannel) && !ClearChatCommand.hasBypassPermission(recipient, clearChannel.id())) ClearChatCommand.sendClear(recipient); return;
            }
            if (PacketKind.CHAT.name().equals(kind)) {
                Channel chatChannel = Channel.fromId(input.readUTF()); if (chatChannel == null || chatChannel == Channel.LOCAL) return;
                UUID senderId = UUID.fromString(input.readUTF()); input.readUTF(); boolean mentionsAllowed = input.readBoolean(); String rawMessage = input.readUTF(); Component output = gson.deserialize(input.readUTF());
                if (plugin.getServer().getPlayer(senderId) != null) return;
                if (mentionsAllowed && plugin.getPluginConfig().getBoolean("mentions.enabled", true)) output = highlightMentions(output);
                for (Player recipient : Bukkit.getOnlinePlayers()) if (shouldReceive(recipient, senderId, chatChannel)) { recipient.sendMessage(output); playRemoteMentionSound(recipient, rawMessage, mentionsAllowed); }
                return;
            }
            if (PacketKind.ALERT.name().equals(kind)) {
                String alertType = input.readUTF(); UUID senderId = UUID.fromString(input.readUTF()); String output = input.readUTF(); if (plugin.getServer().getPlayer(senderId) != null) return;
                for (Player recipient : Bukkit.getOnlinePlayers()) if (plugin.getAlertManager().canReceive(recipient, alertType)) recipient.sendMessage(output);
            }
        } catch (JsonParseException | IOException | IllegalArgumentException e) { plugin.getLogger().warning("Malformed ThunderChat network message: " + e.getMessage()); }
    }
    private void playRemoteMentionSound(Player recipient, String message, boolean allowed) {
        if (!allowed || !plugin.getPluginConfig().getBoolean("mentions.enabled", true)) return;
        if (!message.matches("(?s).*?(?i)(?<![A-Za-z0-9_])@" + Pattern.quote(recipient.getName()) + "\\b.*")) return;
        try { Sound sound = resolveSound(plugin.getPluginConfig().getString("mentions.sound", "ENTITY_EXPERIENCE_ORB_PICKUP")); if (sound != null) recipient.playSound(recipient.getLocation(), sound, 1.0f, 1.0f); } catch (IllegalArgumentException ignored) { }
    }
    private static Sound resolveSound(String name) {
        if (name == null || name.isBlank()) return null;
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT))); if (sound != null) return sound;
        return Sound.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
