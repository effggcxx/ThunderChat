package me.ehsan.thunderchat.spy;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class SpyListener implements Listener {
    private final ThunderChat plugin;
    public SpyListener(ThunderChat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        if (command.startsWith("/")) command = command.substring(1);
        plugin.getSpyManager().spyCommand(event.getPlayer(), command);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getSpyManager().autoEnable(event.getPlayer());
    }
}
