package me.ehsan.thunderchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final ThunderChat plugin;
    public ChatListener(ThunderChat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plugin.getChatColorManager().isAwaitingCustomFormat(player)) {
            event.setCancelled(true);
            final String formatMessage = message;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!plugin.getChatColorManager().isAwaitingCustomFormat(player)) return;
                plugin.getChatColorManager().cancelCustomFormat(player);
                if (!plugin.getChatColorManager().canUseCustom(player)) {
                    player.sendMessage(Component.text("You no longer have permission to use custom formatting.", NamedTextColor.RED));
                    return;
                }
                if (!plugin.getChatColorManager().isValidCustomFormat(formatMessage)) {
                    player.sendMessage(Component.text("Invalid MiniMessage format. Your custom format was not applied.", NamedTextColor.RED));
                    return;
                }
                plugin.getChatColorManager().setCustomFormat(player, formatMessage);
                player.sendMessage(Component.text("Custom chat formatting applied.", NamedTextColor.GREEN));
            });
            return;
        }

        if (plugin.getFilterManager().shouldBlock(player, message)) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getCapsManager().canBypass(player) && plugin.getCapsManager().isAllCaps(message)) {
            plugin.getAlertManager().alert("caps", player, message);
            message = plugin.getCapsManager().normalize(message);
            plugin.getCapsManager().notifyPlayer(player);
        }
        final String finalMessage = message;
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGlobalChatManager().send(player, finalMessage));
    }
}
