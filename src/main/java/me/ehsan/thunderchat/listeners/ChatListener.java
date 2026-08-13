package me.ehsan.thunderchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final ThunderChat plugin;

    public ChatListener(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getChannelManager().sendChat(player, message));
    }
}
