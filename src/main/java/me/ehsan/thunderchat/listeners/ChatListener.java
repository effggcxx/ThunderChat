package me.ehsan.thunderchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final ThunderChat plugin;

    public ChatListener(ThunderChat plugin) {
        this.plugin = plugin;
    }

    // Using Paper's async chat event (not the deprecated Bukkit AsyncPlayerChatEvent).
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        // TODO: 1. run FilterManager checks (spam/caps/words) -> cancel + notify if blocked
        // TODO: 2. resolve sender's active channel via ChannelManager
        // TODO: 3. apply format (prefix/suffix, gradient) via config
        // TODO: 4. scan for @mentions and ping matched players
        // TODO: 5. route final component only to channel recipients
    }
}
