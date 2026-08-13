package me.ehsan.thunderchat.channels;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns active chat channels and routes local/network chat.
 * Network chat uses Velocity's BungeeCord-compatible Forward message.
 */
public class ChannelManager implements PluginMessageListener {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String NETWORK_SUBCHANNEL = "ThunderChat";

    public enum Channel {
        LOCAL("local", "LOCAL CHAT", false),
        DONATOR("donator", "DONATOR CHAT", true),
        STAFF("staff", "STAFF CHAT", true);

        private final String id;
        private final String display;
        private final boolean network;

        Channel(String id, String display, boolean network) {
            this.id = id;
            this.display = display;
            this.network = network;
        }

        public String getId() { return id; }
        public String getDisplayName() { return display; }
        public boolean isNetwork() { return network; }

        public static Channel fromId(String id) {
            for (Channel channel : values()) {
                if (channel.id.equalsIgnoreCase(id)) return channel;
            }
            return null;
        }
    }

    private final ThunderChat plugin;
    private final Map<UUID, Channel> activeChannels = new HashMap<>();

    public ChannelManager(ThunderChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public Channel getActiveChannel(Player player) {
        return activeChannels.getOrDefault(player.getUniqueId(), Channel.LOCAL);
    }

    public void setActiveChannel(Player player, Channel channel) {
        activeChannels.put(player.getUniqueId(), channel);
    }

    public boolean canUse(Player player, Channel channel) {
        return switch (channel) {
            case LOCAL -> true;
            case DONATOR -> player.hasPermission(plugin.getPluginConfig().getString(
                    "channels.donator.permission", "thunderchat.channel.donator"));
            case STAFF -> player.hasPermission(plugin.getPluginConfig().getString(
                    "channels.staff.permission", "thunderchat.channel.staff"));
        };
    }

    public void sendChat(Player sender, String message) {
        Channel channel = getActiveChannel(sender);
        String prefix = resolvePrefix(sender);
        String serverName = plugin.getPluginConfig().getString("network.server-name", "server");

        sendToLocalRecipients(channel, sender.getName(), serverName, prefix, message);
        if (channel != Channel.LOCAL) sendNetwork(channel, sender, prefix, message);
    }

    private void sendToLocalRecipients(Channel channel, String playerName, String serverName,
                                       String prefix, String message) {
        String defaultFormat = channel == Channel.LOCAL
                ? "{prefix}{player}&7: &f{message}"
                : "&7[{server}] {prefix}{player}&7: &f{message}";
        String format = plugin.getPluginConfig().getString(
                channel == Channel.LOCAL ? "format.normal" : "format.global", defaultFormat);

        String formatted = format.replace("{server}", serverName)
                .replace("{prefix}", prefix)
                .replace("{player}", playerName)
                .replace("{message}", message);
        formatted = ChatColor.translateAlternateColorCodes('&', formatted);

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (channel == Channel.LOCAL || canUse(recipient, channel)) recipient.sendMessage(formatted);
        }
    }

    private void sendNetwork(Channel channel, Player sender, String prefix, String message) {
        if (!plugin.getPluginConfig().getBoolean("network.enabled", true)) return;

        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadBytes);
            payload.writeInt(1);
            payload.writeUTF(channel.getId());
            payload.writeUTF(sender.getUniqueId().toString());
            payload.writeUTF(sender.getName());
            payload.writeUTF(plugin.getPluginConfig().getString("network.server-name", "server"));
            payload.writeUTF(prefix);
            payload.writeUTF(message);
            payload.flush();

            ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
            DataOutputStream outer = new DataOutputStream(outerBytes);
            outer.writeUTF("Forward");
            outer.writeUTF("ALL");
            outer.writeUTF(NETWORK_SUBCHANNEL);
            outer.writeShort(payloadBytes.size());
            outer.write(payloadBytes.toByteArray());
            outer.flush();

            sender.sendPluginMessage(plugin, BUNGEE_CHANNEL, outerBytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not send network chat message: " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player source, byte[] message) {
        if (!BUNGEE_CHANNEL.equals(channel)) return;

        try {
            DataInputStream outer = new DataInputStream(new ByteArrayInputStream(message));
            if (!"ThunderChat".equals(outer.readUTF())) return;

            int length = outer.readUnsignedShort();
            if (length <= 0 || length > outer.available()) return;

            byte[] payload = new byte[length];
            outer.readFully(payload);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));

            if (input.readInt() != 1) return;
            Channel chatChannel = Channel.fromId(input.readUTF());
            if (chatChannel == null || chatChannel == Channel.LOCAL) return;

            UUID senderId = UUID.fromString(input.readUTF());
            String playerName = input.readUTF();
            String serverName = input.readUTF();
            String prefix = input.readUTF();
            String chatMessage = input.readUTF();

            String format = plugin.getPluginConfig().getString("format.global",
                    "&7[{server}] {prefix}{player}&7: &f{message}");
            String formatted = ChatColor.translateAlternateColorCodes('&', format
                    .replace("{server}", serverName)
                    .replace("{prefix}", prefix)
                    .replace("{player}", playerName)
                    .replace("{message}", chatMessage));

            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (canUse(recipient, chatChannel)) recipient.sendMessage(formatted);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Received malformed ThunderChat network message.");
        }
    }

    private String resolvePrefix(Player player) {
        String placeholder = plugin.getPluginConfig().getString("format.prefix-placeholder", "");
        if (placeholder.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "";
        return PlaceholderAPI.setPlaceholders(player, placeholder);
    }
}