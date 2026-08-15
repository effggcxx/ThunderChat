package me.ehsan.thunderchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
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
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plugin.getFilterManager().shouldBlock(player, message)) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) {
            message = plugin.getCapsManager().normalize(message);
            plugin.getCapsManager().notifyPlayer(player);
        }

        final String finalMessage = message;
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            GlobalChatManager channels = plugin.getGlobalChatManager();
            channels.send(player, finalMessage);
        });
    }
}
