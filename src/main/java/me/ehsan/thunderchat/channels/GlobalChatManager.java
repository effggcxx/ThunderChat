package me.ehsan.thunderchat.channels;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.commands.ClearChatCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/** Unified channel state for local and network chat. */
public final class GlobalChatManager implements PluginMessageListener {
    private static final int PROTOCOL_VERSION = 1;
    private enum PacketKind { CHAT, CLEAR, ALERT }
    public enum Channel { LOCAL("local", "LOCAL CHAT", false), GLOBAL("global", "GLOBAL CHAT", true), DONATOR("donator", "DONATOR CHAT", true), STAFF("staff", "STAFF CHAT", true), ADMIN("admin", "ADMIN CHAT", true), HIGHRANK("highrank", "HIGH RANK CHAT", true);
        private final String id; private final String display; private final boolean network;
        Channel(String id, String display, boolean network) { this.id = id; this.display = display; this.network = network; }
        public String id() { return id; } public String display() { return display; } public boolean isNetwork() { return network; }
        public static Channel fromId(String id) { if (id == null) return null; for (Channel c : values()) if (c.id.equalsIgnoreCase(id)) return c; return null; }
    }
    private static GlobalChatManager instance;
    private final ThunderChat plugin;
    private final Map<UUID, Channel> active = new HashMap<>();
    private final Map<UUID, EnumSet<Channel>> hidden = new HashMap<>();
    private final File stateFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final LegacyComponentSerializer legacyOutput = LegacyComponentSerializer.legacySection();

    public GlobalChatManager(ThunderChat plugin) { this.plugin = plugin; instance = this; this.stateFile = new File(plugin.getDataFolder(), "channel-state.yml"); loadState(); plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this); }
    public static GlobalChatManager getInstance() { return instance; }
    public Channel get(Player player) { return active.getOrDefault(player.getUniqueId(), Channel.LOCAL); }
    public void set(Player player, Channel channel) { active.put(player.getUniqueId(), channel == null ? Channel.LOCAL : channel); savePlayerState(player.getUniqueId()); }
    public void toggle(Player player, Channel channel) { set(player, get(player) == channel ? Channel.LOCAL : channel); }
    public boolean canUse(Player player, Channel channel) { if (channel == Channel.LOCAL) return true; return player.hasPermission(plugin.getPluginConfig().getString("channels." + channel.id + ".permission", "thunderchat.channel." + channel.id)); }
    public boolean isHidden(Player player, Channel channel) { return hidden.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Channel.class)).contains(channel); }
    public void setHidden(Player player, Channel channel, boolean value) { EnumSet<Channel> channels = hidden.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Channel.class)); if (value) channels.add(channel); else channels.remove(channel); if (get(player) == channel && value) active.put(player.getUniqueId(), Channel.LOCAL); if (channels.isEmpty()) hidden.remove(player.getUniqueId()); savePlayerState(player.getUniqueId()); }
    public void toggleHidden(Player player, Channel channel) { setHidden(player, channel, !isHidden(player, channel)); }
    public void hideAll(Player player) { hidden.put(player.getUniqueId(), EnumSet.allOf(Channel.class)); active.put(player.getUniqueId(), Channel.LOCAL); savePlayerState(player.getUniqueId()); }
    public void showAll(Player player) { hidden.remove(player.getUniqueId()); savePlayerState(player.getUniqueId()); }
    public List<Channel> getAvailableChannels(Player player) { List<Channel> result = new ArrayList<>(); for (Channel c : Channel.values()) if (canUse(player, c)) result.add(c); return result; }
    public void clearPlayer(UUID id) { active.remove(id); hidden.remove(id); }
    public void restorePlayer(Player player) { UUID id = player.getUniqueId(); Channel channel = active.getOrDefault(id, Channel.LOCAL); if (!canUse(player, channel) || isHidden(player, channel)) active.put(id, Channel.LOCAL); }
    private void loadState() { active.clear(); hidden.clear(); if (!"yaml".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.type", "yaml")) || !stateFile.exists()) return; YamlConfiguration data = YamlConfiguration.loadConfiguration(stateFile); for (String rawId : data.getConfigurationSection("players") == null ? Collections.<String>emptySet() : data.getConfigurationSection("players").getKeys(false)) { try { UUID id = UUID.fromString(rawId); Channel channel = Channel.fromId(data.getString("players." + rawId + ".active", "local")); active.put(id, channel == null ? Channel.LOCAL : channel); EnumSet<Channel> hiddenChannels = EnumSet.noneOf(Channel.class); for (String hiddenId : data.getStringList("players." + rawId + ".hidden")) { Channel hiddenChannel = Channel.fromId(hiddenId); if (hiddenChannel != null) hiddenChannels.add(hiddenChannel); } if (!hiddenChannels.isEmpty()) hidden.put(id, hiddenChannels); } catch (IllegalArgumentException ignored) { } } }
    private synchronized void savePlayerState(UUID id) { if (!"yaml".equalsIgnoreCase(plugin.getPluginConfig().getString("storage.type", "yaml"))) return; YamlConfiguration data = stateFile.exists() ? YamlConfiguration.loadConfiguration(stateFile) : new YamlConfiguration(); String path = "players." + id; data.set(path + ".active", active.getOrDefault(id, Channel.LOCAL).id); EnumSet<Channel> channels = hidden.get(id); data.set(path + ".hidden", channels == null ? Collections.emptyList() : channels.stream().map(Channel::id).toList()); try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(stateFile); } catch (IOException e) { plugin.getLogger().warning("Could not save channel-state.yml: " + e.getMessage()); } }

    public void send(Player player, String text) {
        Channel channel = get(player); if (!canUse(player, channel) || isHidden(player, channel)) { set(player, Channel.LOCAL); channel = Channel.LOCAL; }
        if (plugin.getMuteManager().isMuted(player, channel.id)) { player.sendMessage(ChatColor.RED + "That chat is currently muted for you."); return; }
        String server = plugin.getPluginConfig().getString("network.server-name", "server"); String preparedMessage = plugin.getChatColorManager().colorize(player, applyMentionHighlight(player, text));
        String output = format(getFormat(channel), channel, server, prefix(player), player.getName(), preparedMessage, player);
        for (Player recipient : Bukkit.getOnlinePlayers()) if (shouldReceive(recipient, player.getUniqueId(), channel)) { recipient.sendMessage(output); playMentionSoundIfNeeded(recipient, text, player); }
        if (channel.isNetwork()) forwardChat(player, channel, player.getUniqueId(), output);
    }
    private boolean shouldReceive(Player recipient, UUID senderId, Channel channel) { if (isHidden(recipient, channel)) return false; if (channel != Channel.LOCAL && !canUse(recipient, channel)) return false; return !plugin.getPluginConfig().getBoolean("ignore.public-chat.enabled", false) || !plugin.getIgnoreManager().isIgnoring(recipient.getUniqueId(), senderId); }
    private String applyMentionHighlight(Player sender, String message) { if (!plugin.getPluginConfig().getBoolean("mentions.enabled", true) || !sender.hasPermission("thunderchat.mention")) return message; String color = ChatColor.translateAlternateColorCodes('&', plugin.getPluginConfig().getString("mentions.highlight-color", "&e")); for (Player target : Bukkit.getOnlinePlayers()) message = message.replaceAll("(?i)(?<![A-Za-z0-9_])@" + Pattern.quote(target.getName()) + "\\b", java.util.regex.Matcher.quoteReplacement(color + "@" + target.getName() + ChatColor.RESET)); return message; }
    private void playMentionSoundIfNeeded(Player recipient, String message, Player sender) { if (!plugin.getPluginConfig().getBoolean("mentions.enabled", true) || !sender.hasPermission("thunderchat.mention")) return; if (!message.matches("(?s).*?(?i)(?<![A-Za-z0-9_])@" + Pattern.quote(recipient.getName()) + "\\b.*")) return; try { Sound sound = Sound.valueOf(plugin.getPluginConfig().getString("mentions.sound", "ENTITY_EXPERIENCE_ORB_PICKUP")); recipient.playSound(recipient.getLocation(), sound, 1.0f, 1.0f); } catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Invalid mentions.sound in config: " + plugin.getPluginConfig().getString("mentions.sound")); } }
    public void sendAlert(String type, Player source, String blockedMessage) { String output = plugin.getAlertManager().format(type, plugin.getPluginConfig().getString("network.server-name", "server"), source.getName(), blockedMessage); sendAlertLocally(type, output); if (plugin.getPluginConfig().getBoolean("alerts.broadcast-network", true)) try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.ALERT.name()); data.writeUTF(type); data.writeUTF(source.getUniqueId().toString()); data.writeUTF(output); data.flush(); plugin.getNetworkMessenger().forwardAll(source, bytes.toByteArray()); } catch (IOException e) { plugin.getLogger().warning("Could not forward filter alert: " + e.getMessage()); } }
    private void sendAlertLocally(String type, String output) { for (Player recipient : Bukkit.getOnlinePlayers()) if (plugin.getAlertManager().canReceive(recipient, type)) recipient.sendMessage(output); }
    public void clearChat(Channel channel, Player source) { if (channel == Channel.LOCAL) { for (Player recipient : Bukkit.getOnlinePlayers()) if (!isHidden(recipient, Channel.LOCAL) && !ClearChatCommand.hasBypassPermission(recipient, "local")) ClearChatCommand.sendClear(recipient); source.sendMessage(ChatColor.GREEN + "Chat cleared for this gamemode."); return; } for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, channel) && !isHidden(recipient, channel) && !ClearChatCommand.hasBypassPermission(recipient, channel.id)) ClearChatCommand.sendClear(recipient); forwardClear(source, channel); source.sendMessage(ChatColor.GREEN + "Cleared " + channel.display.toLowerCase(Locale.ROOT) + "."); }
    private String getFormat(Channel channel) { String configured = plugin.getPluginConfig().getString("format.channels." + channel.id); if (configured != null && !configured.isEmpty()) return configured; if (channel == Channel.LOCAL) return plugin.getPluginConfig().getString("format.normal", "{prefix}&r{player}&7: &f{message}"); return plugin.getPluginConfig().getString("format.global", "&7[{channel}]&r &7[{server}]&r {prefix}&r{player}&r&7: &f{message}"); }

    /**
     * Renders channel formats with either the legacy ampersand syntax or MiniMessage.
     * MiniMessage is selected when the configured format contains a MiniMessage tag.
     * The message/prefix are inserted as Components so existing Chat Color output is preserved.
     */
    private String format(String format, Channel channel, String server, String prefix, String player, String message, Player placeholderPlayer) {
        String resolved = format.replace("{channel}", channel.display).replace("{server}", server).replace("{player}", player);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) resolved = PlaceholderAPI.setPlaceholders(placeholderPlayer, resolved);

        boolean miniMessageFormat = resolved.matches("(?s).*<[/!?#a-zA-Z][^>]*>.*");
        if (!miniMessageFormat) {
            resolved = resolved.replace("{prefix}", prefix).replace("{message}", message);
            return ChatColor.translateAlternateColorCodes('&', resolved);
        }

        TagResolver resolver = TagResolver.resolver(
                TagResolver.resolver("tc_prefix", net.kyori.adventure.text.minimessage.tag.Tag.inserting(legacy.deserialize(prefix))),
                TagResolver.resolver("tc_message", net.kyori.adventure.text.minimessage.tag.Tag.inserting(legacy.deserialize(message)))
        );
        String miniFormat = resolved.replace("{prefix}", "<tc_prefix>").replace("{message}", "<tc_message>");
        try {
            Component component = miniMessage.deserialize(miniFormat, TagResolver.resolver(StandardTags.color(), StandardTags.decorations(), StandardTags.gradient(), resolver));
            return legacyOutput.serialize(component);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid MiniMessage channel format: " + format + " (" + ex.getMessage() + ")");
            resolved = resolved.replace("{prefix}", prefix).replace("{message}", message);
            return ChatColor.translateAlternateColorCodes('&', resolved);
        }
    }
    private String prefix(Player player) { String template = plugin.getPluginConfig().getString("format.prefix-placeholder", "%luckperms_prefix% "); if (template == null || template.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return ""; return PlaceholderAPI.setPlaceholders(player, template); }
    private void forwardChat(Player player, Channel channel, UUID senderId, String resolvedOutput) { try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.CHAT.name()); data.writeUTF(channel.id); data.writeUTF(senderId.toString()); data.writeUTF(resolvedOutput); data.flush(); plugin.getNetworkMessenger().forwardAll(player, bytes.toByteArray()); } catch (IOException e) { plugin.getLogger().warning("Could not forward global chat: " + e.getMessage()); } }
    private void forwardClear(Player player, Channel channel) { try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(PROTOCOL_VERSION); data.writeUTF(PacketKind.CLEAR.name()); data.writeUTF(channel.id); data.flush(); plugin.getNetworkMessenger().forwardAll(player, bytes.toByteArray()); } catch (IOException e) { plugin.getLogger().warning("Could not forward chat clear: " + e.getMessage()); } }
    @Override public void onPluginMessageReceived(String channel, Player source, byte[] data) { if (!"BungeeCord".equals(channel) || !plugin.getPluginConfig().getBoolean("network.enabled", true)) return; try { DataInputStream outer = new DataInputStream(new ByteArrayInputStream(data)); if (!"ThunderChat".equals(outer.readUTF())) return; int length = outer.readUnsignedShort(); if (length <= 0 || length > outer.available()) return; byte[] payload = new byte[length]; outer.readFully(payload); DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload)); int version = input.readInt(); if (version != PROTOCOL_VERSION) return; String kind = input.readUTF(); if (PacketKind.CLEAR.name().equals(kind)) { Channel clearChannel = Channel.fromId(input.readUTF()); if (clearChannel == null || clearChannel == Channel.LOCAL) return; for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, clearChannel) && !isHidden(recipient, clearChannel) && !ClearChatCommand.hasBypassPermission(recipient, clearChannel.id)) ClearChatCommand.sendClear(recipient); return; } if (PacketKind.CHAT.name().equals(kind)) { Channel chatChannel = Channel.fromId(input.readUTF()); if (chatChannel == null || chatChannel == Channel.LOCAL) return; UUID senderId = UUID.fromString(input.readUTF()); String output = input.readUTF(); if (plugin.getServer().getPlayer(senderId) != null) return; for (Player recipient : Bukkit.getOnlinePlayers()) if (shouldReceive(recipient, senderId, chatChannel)) recipient.sendMessage(output); return; } if (PacketKind.ALERT.name().equals(kind)) { String alertType = input.readUTF(); UUID senderId = UUID.fromString(input.readUTF()); String output = input.readUTF(); if (plugin.getServer().getPlayer(senderId) != null) return; for (Player recipient : Bukkit.getOnlinePlayers()) if (plugin.getAlertManager().canReceive(recipient, alertType)) recipient.sendMessage(output); } } catch (Exception e) { plugin.getLogger().warning("Malformed or unsupported ThunderChat network message: " + e.getMessage()); } }
}