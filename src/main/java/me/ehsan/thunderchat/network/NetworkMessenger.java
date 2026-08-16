package me.ehsan.thunderchat.network;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Shared network transport for ThunderChat's BungeeCord-compatible packets. */
public final class NetworkMessenger {
    private static final String CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "ThunderChat";

    private final ThunderChat plugin;

    public NetworkMessenger(ThunderChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void forwardAll(Player carrier, byte[] payload) throws IOException {
        if (!plugin.getPluginConfig().getBoolean("network.enabled", true)) return;
        if (carrier == null || payload == null) {
            throw new IOException("A connected player carrier and payload are required");
        }
        if (payload.length > 65535) {
            throw new IOException("ThunderChat network payload exceeds 65535 bytes");
        }

        ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
        DataOutputStream outer = new DataOutputStream(outerBytes);
        outer.writeUTF("Forward");
        outer.writeUTF("ALL");
        outer.writeUTF(SUBCHANNEL);
        outer.writeShort(payload.length);
        outer.write(payload);
        outer.flush();
        carrier.sendPluginMessage(plugin, CHANNEL, outerBytes.toByteArray());
    }
}
