package me.ehsan.thunderchat.channels;

import me.ehsan.thunderchat.ThunderChat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChannelManager {

    private final ThunderChat plugin;
    private final Map<UUID, String> activeChannel = new HashMap<>();

    public ChannelManager(ThunderChat plugin) {
        this.plugin = plugin;
        // TODO: load channel definitions (global/local/staff/...) from config.yml
    }

    // TODO: getActiveChannel(player), setActiveChannel(player, name),
    //       getRecipients(channel, sender) -> radius check for local, permission check for staff
}
