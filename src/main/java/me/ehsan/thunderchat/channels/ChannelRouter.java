package me.ehsan.thunderchat.channels;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChannelRouter implements PluginMessageListener {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "ThunderChat";
    private static final int VERSION = 2;
    protected final ThunderChat plugin;
    private final Map<UUID, ChannelManager.Channel> activeChannels = new HashMap<>();

    public ChannelRouter(ThunderChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public ChannelManager.Channel getActiveChannel(Player player) {
        return activeChannels.getOrDefault(player.getUniqueId(), ChannelManager.Channel.LOCAL);
    }

    public void setActiveChannel(Player player, ChannelManager.Channel channel) {
        activeChannels.put(player.getUniqueId(), channel);
    }

    public boolean canUse(Player player, ChannelManager.Channel channel) {
        return player.hasPermission(plugin.getPluginConfig().getString(
                "channels." + channel.getId() + ".permission",
                channel == ChannelManager.Channel.LOCAL ? "" : "thunderchat.channel." + channel.getId()))
                || channel == ChannelManager.Channel.LOCAL;
    }

    public void sendChat(Player sender, String message) {
        ChannelManager.Channel channel = getActiveChannel(sender);
        if (channel.isNetwork() && plugin.getMuteManager().isMuted(sender, channel.getId())) {
            sender.sendMessage(ChatColor.RED + "That chat is currently muted for you.");
            return;
        }
        String prefix = resolvePrefix(sender);
        String server = plugin.getPluginConfig().getString("network.server-name", "server");
        String format = plugin.getPluginConfig().getString(
                channel == ChannelManager.Channel.LOCAL ? "format.normal" : "format.global",
                channel == ChannelManager.Channel.LOCAL
                        ? "{prefix}{player}&7: &f{message}"
                        : "&7[{channel}] [{server}] {prefix}{player}&7: &f{message}");
        String formatted = format(format, channel, server, prefix, sender.getName(), message);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (channel == ChannelManager.Channel.LOCAL
                    || (canUse(recipient, channel) && !plugin.getMuteManager().isMuted(recipient, channel.getId()))) {
                recipient.sendMessage(formatted);
            }
        }
        if (channel.isNetwork()) sendNetwork(channel, sender, prefix, message);
    }

    private String format(String format, ChannelManager.Channel channel, String server,
                          String prefix, String player, String message) {
        return ChatColor.translateAlternateColorCodes('&',
                format.replace("{channel}", channel.getDisplayName())
                        .replace("{server}", server)
                        .replace("{prefix}", prefix)
                        .replace("{player}", player)
                        .replace("{message}", message));
    }

    private void sendNetwork(ChannelManager.Channel channel, Player sender, String prefix, String message) {
        if (!plugin.getPluginConfig().getBoolean("network.enabled", true)) return;
        try {
            ByteArrayOutputStream p = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(p);
            out.writeInt(VERSION);
            out.writeUTF("CHAT");
            out.writeUTF(channel.getId());
            out.writeUTF(sender.getUniqueId().toString());
            out.writeUTF(sender.getName());
            out.writeUTF(plugin.getPluginConfig().getString("network.server-name", "server"));
            out.writeUTF(prefix);
            out.writeUTF(message);
            out.flush();
            sendForward(p.toByteArray(), sender);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not send network chat: " + e.getMessage());
        }
    }

    private void sendForward(byte[] payload, Player carrier) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(SUBCHANNEL);
        out.writeShort(payload.length);
        out.write(payload);
        out.flush();
        carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, bytes.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player source, byte[] message) {
        if (!BUNGEE_CHANNEL.equals(channel)) return;
        try {
            DataInputStream outer = new DataInputStream(new ByteArrayInputStream(message));
            if (!SUBCHANNEL.equals(outer.readUTF())) return;
            int length = outer.readUnsignedShort();
            if (length <= 0 || length > outer.available()) return;
            byte[] payload = new byte[length];
            outer.readFully(payload);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            if (in.readInt() != VERSION || !"CHAT".equals(in.readUTF())) return;
            ChannelManager.Channel chatChannel = ChannelManager.Channel.fromId(in.readUTF());
            if (chatChannel == null || !chatChannel.isNetwork()) return;
            in.readUTF();
            String player = in.readUTF();
            String server = in.readUTF();
            String prefix = in.readUTF();
            String text = in.readUTF();
            String format = plugin.getPluginConfig().getString(
                    "format.global", "&7[{channel}] [{server}] {prefix}{player}&7: &f{message}");
            String formatted = format(format, chatChannel, server, prefix, player, text);
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (canUse(recipient, chatChannel)
                        && !plugin.getMuteManager().isMuted(recipient, chatChannel.getId())) {
                    recipient.sendMessage(formatted);
                }
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