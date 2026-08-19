package me.ehsan.thunderchat.network;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.List;

/** Single ThunderChat network transport/dispatcher for BungeeCord-compatible forwarding. */
public final class NetworkMessenger implements PluginMessageListener {
    private static final String CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "ThunderChat";
    private final ThunderChat plugin;

    public NetworkMessenger(ThunderChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void forwardAll(Player carrier, byte[] payload) throws IOException {
        if (!plugin.getPluginConfig().getBoolean("network.enabled", true)) return;
        if (payload == null || payload.length > 65535) throw new IOException("Invalid ThunderChat network payload");
        if (carrier == null) throw new IOException("No connected player carrier is available");
        sendRaw(carrier, payload);
        queueForConfiguredBackends(payload);
    }

    public void sendPrivateMessage(Player carrier, UUID senderId, String senderName, String targetName, UUID targetId, String message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(1); out.writeUTF("PM"); out.writeUTF(senderId.toString()); out.writeUTF(senderName);
        out.writeBoolean(targetId != null); if (targetId != null) out.writeUTF(targetId.toString());
        out.writeUTF(targetName); out.writeUTF(message); out.flush();
        forwardAll(carrier, bytes.toByteArray());
    }

    private void sendRaw(Player carrier, byte[] payload) throws IOException {
        ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
        DataOutputStream outer = new DataOutputStream(outerBytes);
        outer.writeUTF("Forward"); outer.writeUTF("ALL"); outer.writeUTF(SUBCHANNEL);
        outer.writeShort(payload.length); outer.write(payload); outer.flush();
        carrier.sendPluginMessage(plugin, CHANNEL, outerBytes.toByteArray());
    }

    private void queueForConfiguredBackends(byte[] payload) {
        if (!plugin.getStorage().isEnabled()) return;
        String local = plugin.getPluginConfig().getString("network.server-name", "server");
        List<String> servers = plugin.getPluginConfig().getStringList("network.servers");
        for (String server : servers) if (!server.equalsIgnoreCase(local) && !server.isBlank()) plugin.getStorage().enqueueNetwork(server, payload);
    }

    @Override public void onPluginMessageReceived(String channel, Player source, byte[] data) {
        if (!CHANNEL.equals(channel) || !plugin.getPluginConfig().getBoolean("network.enabled", true) || data == null) return;
        dispatch(data, source);
    }

    public void dispatch(byte[] data) { dispatch(data, null); }

    private void dispatch(byte[] data, Player source) {
        try {
            DataInputStream outer = new DataInputStream(new ByteArrayInputStream(data));
            if (!"ThunderChat".equals(readUtfBounded(outer, 64))) return;
            int length = outer.readUnsignedShort(); if (length <= 0 || length > outer.available()) return;
            byte[] payload = outer.readNBytes(length); DataInputStream packet = new DataInputStream(new ByteArrayInputStream(payload));
            if (packet.readInt() != 1) return; String kind = readUtfBounded(packet, 32);
            if ("MUTE".equals(kind)) { plugin.getMuteManager().onNetworkPacket("BungeeCord", source, payload); return; }
            if ("PM".equals(kind)) { plugin.getMessageManager().onNetworkPacket(payload); return; }
            if ("CHAT".equals(kind) || "CLEAR".equals(kind) || "ALERT".equals(kind)) plugin.getGlobalChatManager().onNetworkPacket("BungeeCord", source, payload);
        } catch (IOException | RuntimeException e) { plugin.getLogger().warning("Malformed ThunderChat network packet: " + e.getMessage()); }
    }

    private String readUtfBounded(DataInputStream input, int max) throws IOException { String value = input.readUTF(); if (value.length() > max) throw new IOException("network string too long"); return value; }
    public void drainQueuedPackets() { if (!plugin.getStorage().isEnabled()) return; String server = plugin.getPluginConfig().getString("network.server-name", "server"); plugin.getStorage().drainNetwork(server, this::dispatch); }
    public void close() { plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this); plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL); }
}
