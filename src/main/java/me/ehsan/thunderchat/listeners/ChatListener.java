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
    public ChatListener(ThunderChat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plugin.getFilterManager().shouldBlock(player, message)) {
            plugin.getSpyManager().spyChat(player, plugin.getGlobalChatManager().get(player).id(), message);
            event.setCancelled(true);
            return;
        }
        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) {
            plugin.getSpyManager().spyChat(player, plugin.getGlobalChatManager().get(player).id(), message);
            message = plugin.getCapsManager().normalize(message);
            plugin.getCapsManager().notifyPlayer(player);
        }
        final String finalMessage = message;
        final String channel = plugin.getGlobalChatManager().get(player).id();
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getGlobalChatManager().send(player, finalMessage);
            plugin.getSpyManager().spyChat(player, channel, finalMessage);
        });
    }
}
