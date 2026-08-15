package me.ehsan.thunderchat.channels;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.alerts.AlertManager;
import me.ehsan.thunderchat.commands.ClearChatCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;

/** Unified channel state for local and network chat. */
public final class GlobalChatManager implements PluginMessageListener {
    public enum Channel {
        LOCAL("local", "LOCAL CHAT", false), GLOBAL("global", "GLOBAL CHAT", true),
        DONATOR("donator", "DONATOR CHAT", true), STAFF("staff", "STAFF CHAT", true),
        ADMIN("admin", "ADMIN CHAT", true), HIGHRANK("highrank", "HIGH RANK CHAT", true);
        private final String id; private final String display; private final boolean network;
        Channel(String id, String display, boolean network) { this.id = id; this.display = display; this.network = network; }
        public String id() { return id; } public String display() { return display; } public boolean isNetwork() { return network; }
        public static Channel fromId(String id) { if (id == null) return null; for (Channel c : values()) if (c.id.equalsIgnoreCase(id)) return c; return null; }
    }

    private static GlobalChatManager instance;
    private final ThunderChat plugin;
    private final Map<UUID, Channel> active = new HashMap<>();
    private final Map<UUID, EnumSet<Channel>> hidden = new HashMap<>();

    public GlobalChatManager(ThunderChat plugin) {
        this.plugin = plugin; instance = this;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }
    public static GlobalChatManager getInstance() { return instance; }
    public Channel get(Player player) { return active.getOrDefault(player.getUniqueId(), Channel.LOCAL); }
    public void set(Player player, Channel channel) { active.put(player.getUniqueId(), channel == null ? Channel.LOCAL : channel); }
    public void toggle(Player player, Channel channel) { set(player, get(player) == channel ? Channel.LOCAL : channel); }
    public boolean canUse(Player player, Channel channel) {
        if (channel == Channel.LOCAL) return true;
        return player.hasPermission(plugin.getPluginConfig().getString("channels." + channel.id + ".permission", "thunderchat.channel." + channel.id));
    }
    public boolean isHidden(Player player, Channel channel) { return hidden.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Channel.class)).contains(channel); }
    public void setHidden(Player player, Channel channel, boolean value) {
        EnumSet<Channel> channels = hidden.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Channel.class));
        if (value) channels.add(channel); else channels.remove(channel);
        if (get(player) == channel && value) set(player, Channel.LOCAL);
        if (channels.isEmpty()) hidden.remove(player.getUniqueId());
    }
    public void toggleHidden(Player player, Channel channel) { setHidden(player, channel, !isHidden(player, channel)); }
    public void hideAll(Player player) { hidden.put(player.getUniqueId(), EnumSet.allOf(Channel.class)); set(player, Channel.LOCAL); }
    public void showAll(Player player) { hidden.remove(player.getUniqueId()); }
    public List<Channel> getAvailableChannels(Player player) { List<Channel> result = new ArrayList<>(); for (Channel c : Channel.values()) if (canUse(player, c)) result.add(c); return result; }

    public void send(Player player, String text) {
        Channel channel = get(player);
        if (!canUse(player, channel) || isHidden(player, channel)) { set(player, Channel.LOCAL); channel = Channel.LOCAL; }
        if (plugin.getMuteManager().isMuted(player, channel.id)) { player.sendMessage(ChatColor.RED + "That chat is currently muted for you."); return; }
        String server = plugin.getPluginConfig().getString("network.server-name", "server");
        String prefix = prefix(player);
        String output = format(getFormat(channel), channel, server, prefix, player.getName(), text, player);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (channel == Channel.LOCAL) { if (!isHidden(recipient, Channel.LOCAL)) recipient.sendMessage(output); }
            else if (canUse(recipient, channel) && !isHidden(recipient, channel)) recipient.sendMessage(output);
        }
        if (channel.isNetwork()) forwardChat(player, channel, output);
    }

    /** Sends a filter alert to every eligible viewer on this server and forwards it network-wide. */
    public void sendAlert(String type, Player source, String blockedMessage) {
        String server = plugin.getPluginConfig().getString("network.server-name", "server");
        String output = plugin.getAlertManager().format(type, server, source.getName(), blockedMessage);
        sendAlertLocally(type, output);
        if (plugin.getPluginConfig().getBoolean("alerts.broadcast-network", true)) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream data = new DataOutputStream(bytes);
                data.writeInt(4); data.writeUTF("ALERT"); data.writeUTF(type); data.writeUTF(output); data.flush();
                sendNetwork(source, bytes.toByteArray());
            } catch (IOException e) { plugin.getLogger().warning("Could not forward filter alert: " + e.getMessage()); }
        }
    }

    private void sendAlertLocally(String type, String output) {
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (plugin.getAlertManager().canReceive(recipient, type)) recipient.sendMessage(output);
        }
    }

    public void clearChat(Channel channel, Player source) {
        if (channel == Channel.LOCAL) {
            for (Player recipient : Bukkit.getOnlinePlayers()) if (!isHidden(recipient, Channel.LOCAL) && !ClearChatCommand.hasBypassPermission(recipient, "local")) ClearChatCommand.sendClear(recipient);
            source.sendMessage(ChatColor.GREEN + "Chat cleared for this gamemode."); return;
        }
        for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, channel) && !isHidden(recipient, channel) && !ClearChatCommand.hasBypassPermission(recipient, channel.id)) ClearChatCommand.sendClear(recipient);
        forwardClear(source, channel); source.sendMessage(ChatColor.GREEN + "Cleared " + channel.display.toLowerCase(Locale.ROOT) + ".");
    }

    private String getFormat(Channel channel) {
        String configured = plugin.getPluginConfig().getString("format.channels." + channel.id);
        if (configured != null && !configured.isEmpty()) return configured;
        if (channel == Channel.LOCAL) return plugin.getPluginConfig().getString("format.normal", "{prefix}&r{player}&7: &f{message}");
        return plugin.getPluginConfig().getString("format.global", "&7[{channel}]&r &7[{server}]&r {prefix}&r{player}&r&7: &f{message}");
    }
    private String format(String format, Channel channel, String server, String prefix, String player, String message, Player placeholderPlayer) {
        String resolved = format.replace("{channel}", channel.display).replace("{server}", server).replace("{prefix}", prefix).replace("{player}", player).replace("{message}", message);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) resolved = PlaceholderAPI.setPlaceholders(placeholderPlayer, resolved);
        return ChatColor.translateAlternateColorCodes('&', resolved);
    }
    private String prefix(Player player) {
        String template = plugin.getPluginConfig().getString("format.prefix-placeholder", "%luckperms_prefix% ");
        if (template == null || template.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        return PlaceholderAPI.setPlaceholders(player, template);
    }
    private void forwardChat(Player player, Channel channel, String resolvedOutput) {
        try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(3); data.writeUTF("CHAT"); data.writeUTF(channel.id); data.writeUTF(resolvedOutput); data.flush(); sendNetwork(player, bytes.toByteArray()); }
        catch (IOException e) { plugin.getLogger().warning("Could not forward global chat: " + e.getMessage()); }
    }
    private void forwardClear(Player player, Channel channel) {
        try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes); data.writeInt(1); data.writeUTF("CLEAR"); data.writeUTF(channel.id); data.flush(); sendNetwork(player, bytes.toByteArray()); }
        catch (IOException e) { plugin.getLogger().warning("Could not forward chat clear: " + e.getMessage()); }
    }
    private void sendNetwork(Player player, byte[] payload) throws IOException {
        ByteArrayOutputStream outerBytes = new ByteArrayOutputStream(); DataOutputStream outer = new DataOutputStream(outerBytes);
        outer.writeUTF("Forward"); outer.writeUTF("ALL"); outer.writeUTF("ThunderChat"); outer.writeShort(payload.length); outer.write(payload); outer.flush();
        player.sendPluginMessage(plugin, "BungeeCord", outerBytes.toByteArray());
    }

    @Override public void onPluginMessageReceived(String channel, Player source, byte[] data) {
        if (!"BungeeCord".equals(channel)) return;
        try {
            DataInputStream outer = new DataInputStream(new ByteArrayInputStream(data));
            if (!"ThunderChat".equals(outer.readUTF())) return;
            int length = outer.readUnsignedShort(); if (length <= 0 || length > outer.available()) return;
            byte[] payload = new byte[length]; outer.readFully(payload);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            int type = input.readInt(); String kind = input.readUTF();
            if (type == 1 && "CLEAR".equals(kind)) {
                Channel clearChannel = Channel.fromId(input.readUTF()); if (clearChannel == null || clearChannel == Channel.LOCAL) return;
                for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, clearChannel) && !isHidden(recipient, clearChannel) && !ClearChatCommand.hasBypassPermission(recipient, clearChannel.id)) ClearChatCommand.sendClear(recipient);
                return;
            }
            if (type == 3 && "CHAT".equals(kind)) {
                Channel chatChannel = Channel.fromId(input.readUTF()); if (chatChannel == null || chatChannel == Channel.LOCAL) return;
                String output = input.readUTF();
                for (Player recipient : Bukkit.getOnlinePlayers()) if (canUse(recipient, chatChannel) && !isHidden(recipient, chatChannel) && !plugin.getMuteManager().isMuted(recipient, chatChannel.id)) recipient.sendMessage(output);
                return;
            }
            if (type == 4 && "ALERT".equals(kind)) {
                String alertType = input.readUTF(); String output = input.readUTF();
                for (Player recipient : Bukkit.getOnlinePlayers()) if (plugin.getAlertManager().canReceive(recipient, alertType)) recipient.sendMessage(output);
            }
        } catch (Exception e) { plugin.getLogger().warning("Malformed ThunderChat network message."); }
    }
}
