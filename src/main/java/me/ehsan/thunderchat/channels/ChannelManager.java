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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the player's active chat channel and routes local/network chat.
 * Network chat uses Velocity's BungeeCord-compatible Forward message, so
 * no companion Velocity plugin is required when bungee-plugin-message-channel is enabled.
 */
public class ChannelManager implements PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String NETWORK_SUBCHANNEL = "ThunderChat";

    public enum Channel {
        LOCAL("local"),
        DONATOR("donator"),
        STAFF("staff");

        private final String id;

        Channel(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

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
            case DONATOR -> player.hasPermission(plugin.getPluginConfig()
                    .getString("channels.donator.permission", "thunderchat.channel.donator"));
            case STAFF -> player.hasPermission(plugin.getPluginConfig()
                    .getString("channels.staff.permission", "thunderchat.channel.staff"));
        };
    }

    public void sendChat(Player sender, String message) {
        Channel channel = getActiveChannel(sender);
        if (channel == Channel.LOCAL) {
            sendLocal(sender, message);
            return;
        }

        String prefix = resolvePrefix(sender);
        sendToLocalRecipients(channel, sender.getName(), plugin.getPluginConfig()
                .getString("network.server-name", "server"), prefix, message);
        sendNetwork(channel, sender, prefix, message);
    }

    private void sendLocal(Player sender, String message) {
        sendToLocalRecipients(Channel.LOCAL, sender.getName(),
                plugin.getPluginConfig().getString("network.server-name", "server"),
                resolvePrefix(sender), message);
    }

    private void sendToLocalRecipients(Channel channel, String playerName, String serverName,
                                       String prefix, String message) {
        String path = channel == Channel.LOCAL ? "format.normal" : "format.global";
        String format = plugin.getPluginConfig().getString(path,
                channel == Channel.LOCAL ? "{prefix}{player}&7: &f{message}" :
                        "&7[{server}] {prefix}{player}&7: &f{message}");

        String formatted = format
                .replace("{server}", serverName)
                .replace("{prefix}", prefix)
                .replace("{player}", playerName)
                .replace("{message}", message);
        formatted = ChatColor.translateAlternateColorCodes('&', formatted);

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (channel == Channel.LOCAL || canUse(recipient, channel)) {
                recipient.sendMessage(formatted);
            }
        }
    }

    private void sendNetwork(Channel channel, Player sender, String prefix, String message) {
        if (!plugin.getPluginConfig().getBoolean("network.enabled", true)) return;

        byte[] payload;
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
            payloadOut.writeInt(1);
            payloadOut.writeUTF(channel.getId());
            payloadOut.writeUTF(sender.getUniqueId().toString());
            payloadOut.writeUTF(plugin.getPluginConfig().getString("network.server-name", "server"));
            payloadOut.writeUTF(prefix);
            payloadOut.writeUTF(message);
            payloadOut.flush();
            payload = payloadBytes.toByteArray();

            ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
            DataOutputStream outer = new DataOutputStream(outerBytes);
            outer.writeUTF("Forward");
            outer.writeUTF("ALL");
            outer.writeUTF(NETWORK_SUBCHANNEL);
            outer.writeShort(payload.length);
            outer.write(payload);
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
            String subchannel = outer.readUTF();
            if (!NETWORK_SUBCHANNEL.equals(subchannel)) return;

            short length = outer.readShort();
            if (length <= 0 || length > outer.available()) return;

            byte[] payload = new byte[length];
            outer.readFully(payload);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));

            int version = input.readInt();
            if (version != 1) return;

            Channel chatChannel = Channel.fromId(input.readUTF());
            if (chatChannel == null || chatChannel == Channel.LOCAL) return;

            UUID senderId = UUID.fromString(input.readUTF());
            String serverName = input.readUTF();
            String prefix = input.readUTF();
            String chatMessage = input.readUTF();

            // Forwarded messages are already on the main server thread.
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (canUse(recipient, chatChannel)) {
                    String format = plugin.getPluginConfig().getString("format.global",
                            "&7[{server}] {prefix}{player}&7: &f{message}");
                    String formatted = format
                            .replace("{server}", serverName)
                            .replace("{prefix}", prefix)
                            .replace("{player}", findName(senderId))
                            .replace("{message}", chatMessage);
                    recipient.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
                }
            }
        } catch (Exception ignored) {
            plugin.getLogger().warning("Received malformed ThunderChat network message.");
        }
    }

    private String findName(UUID uuid) {
        Player local = Bukkit.getPlayer(uuid);
        return local != null ? local.getName() : "Player";
    }

    private String resolvePrefix(Player player) {
        String placeholder = plugin.getPluginConfig().getString("format.prefix-placeholder", "");
        if (placeholder.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return "";
        }
        return PlaceholderAPI.setPlaceholders(player, placeholder);
    }
}