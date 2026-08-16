package me.ehsan.thunderchat.listeners;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Clears transient per-player runtime state when a player leaves. */
public final class PlayerStateListener implements Listener {
    private final ThunderChat plugin;

    public PlayerStateListener(ThunderChat plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var id = event.getPlayer().getUniqueId();
        plugin.getGlobalChatManager().clearPlayer(id);
        plugin.getFilterManager().clear(id);
        plugin.getMessageManager().clearPlayer(id);
    }
}
