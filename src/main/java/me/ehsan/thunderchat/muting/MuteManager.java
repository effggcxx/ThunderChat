package me.ehsan.thunderchat.muting;

import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.ChannelManager.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks network chat mutes and synchronizes them between Velocity backend servers. */
public final class MuteManager implements PluginMessageListener {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "ThunderChat";
    private static final int VERSION = 2;

    private final ThunderChat plugin;
    private final EnumSet<Channel> globallyMuted = EnumSet.noneOf(Channel.class);
    private final EnumMap<Channel, Set<UUID>> playerMutes = new EnumMap<>(Channel.class);

    public MuteManager(ThunderChat plugin) {
        this.plugin = plugin;
        for (Channel channel : Channel.values()) playerMutes.put(channel, new HashSet<>());
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public boolean isMuted(Player player, Channel channel) {
        if (channel == Channel.LOCAL) return false;
        if (hasBypass(player, channel)) return false;
        return globallyMuted.contains(channel) || globallyMuted.contains(Channel.GLOBAL)
                || playerMutes.get(channel).contains(player.getUniqueId())
                || playerMutes.get(Channel.GLOBAL).contains(player.getUniqueId());
    }

    public boolean hasBypass(Player player, Channel channel) {
        return player.hasPermission("thunderchat.bypass.mute")
                || player.hasPermission("thunderchat.bypass.mute." + channel.getId());
    }

    public void setGlobalMuted(Channel channel, boolean muted) {
        if (channel == Channel.LOCAL) return;
        if (muted) globallyMuted.add(channel); else globallyMuted.remove(channel);
        broadcastState(channel, null, muted);
    }

    public void setPlayerMuted(Channel channel, UUID playerId, boolean muted) {
        if (channel == Channel.LOCAL) return;
        Set<UUID> mutes = playerMutes.get(channel);
        if (muted) mutes.add(playerId); else mutes.remove(playerId);
        broadcastState(channel, playerId, muted);
    }

    private void broadcastState(Channel channel, UUID playerId, boolean muted) {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null || !plugin.getPluginConfig().getBoolean("network.enabled", true)) return;
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(payloadBytes);
            payload.writeInt(VERSION);
            payload.writeUTF("MUTE");
            payload.writeUTF(channel.getId());
            payload.writeBoolean(playerId != null);
            if (playerId != null) payload.writeUTF(playerId.toString());
            payload.writeBoolean(muted);
            payload.flush();

            ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
            DataOutputStream outer = new DataOutputStream(outerBytes);
            outer.writeUTF("Forward");
            outer.writeUTF("ALL");
            outer.writeUTF(SUBCHANNEL);
            outer.writeShort(payloadBytes.size());
            outer.write(payloadBytes.toByteArray());
            outer.flush();
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, outerBytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not synchronize chat mute: " + e.getMessage());
        }
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

            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            if (input.readInt() != VERSION || !"MUTE".equals(input.readUTF())) return;
            Channel muteChannel = Channel.fromId(input.readUTF());
            if (muteChannel == null || muteChannel == Channel.LOCAL) return;
            boolean hasPlayer = input.readBoolean();
            UUID playerId = hasPlayer ? UUID.fromString(input.readUTF()) : null;
            boolean muted = input.readBoolean();

            if (playerId == null) {
                if (muted) globallyMuted.add(muteChannel); else globallyMuted.remove(muteChannel);
            } else {
                if (muted) playerMutes.get(muteChannel).add(playerId);
                else playerMutes.get(muteChannel).remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Received malformed ThunderChat mute message.");
        }
    }
}
