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

/** Handles Paper's asynchronous chat event and hands accepted messages to the channel manager. */
public final class ChatListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final ThunderChat plugin;

    public ChatListener(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PLAIN_TEXT.serialize(event.message());

        if (plugin.getChatColorManager().isAwaitingCustomFormat(player)) {
            handleCustomFormatPrompt(event, player, message);
            return;
        }

        if (plugin.getFilterManager().shouldBlock(player, message)) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getCapsManager().canBypass(player)
                && plugin.getCapsManager().isAllCaps(message)) {
            plugin.getAlertManager().alert("caps", player, message);
            message = plugin.getCapsManager().normalize(message);
            plugin.getCapsManager().notifyPlayer(player);
        }

        final String finalMessage = message;
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> plugin.getGlobalChatManager().send(player, finalMessage)
        );
    }

    private void handleCustomFormatPrompt(AsyncChatEvent event, Player player, String message) {
        event.setCancelled(true);
        final String formatMessage = message;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Atomically claim the one-shot prompt so queued chat events cannot consume it twice.
            if (!plugin.getChatColorManager().consumeAwaitingCustomFormat(player)) {
                return;
            }

            if (!plugin.getChatColorManager().canUseCustom(player)) {
                player.sendMessage(Component.text(
                        "You no longer have permission to use custom formatting.",
                        NamedTextColor.RED
                ));
                return;
            }

            if (!plugin.getChatColorManager().isValidCustomFormat(formatMessage)) {
                player.sendMessage(Component.text(
                        "Invalid MiniMessage format. Your custom format was not applied.",
                        NamedTextColor.RED
                ));
                return;
            }

            plugin.getChatColorManager().setCustomFormat(player, formatMessage);
            player.sendMessage(Component.text(
                    "Custom chat formatting applied.",
                    NamedTextColor.GREEN
            ));
        });
    }
}
