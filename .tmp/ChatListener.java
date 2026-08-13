package me.ehsan.thunderchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ehsan.thunderchat.ThunderChat;
import me.ehsan.thunderchat.channels.GlobalChatManager;
import me.ehsan.thunderchat.channels.GlobalChatManager.Channel;
import me.ehsan.thunderchat.commands.ChatChannelCommand;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final ThunderChat plugin;
    public ChatListener(ThunderChat plugin) {
        this.plugin = plugin;
        new GlobalChatManager(plugin);
        plugin.getCommand("chat").setExecutor(new ChatChannelCommand(plugin, Channel.GLOBAL));
        plugin.getCommand("globalchat").setExecutor(new ChatChannelCommand(plugin, Channel.GLOBAL));
        plugin.getCommand("staffchat").setExecutor(new ChatChannelCommand(plugin, Channel.STAFF));
        plugin.getCommand("donatorchat").setExecutor(new ChatChannelCommand(plugin, Channel.DONATOR));
        plugin.getCommand("adminchat").setExecutor(new ChatChannelCommand(plugin, Channel.ADMIN));
        plugin.getCommand("highrankchat").setExecutor(new ChatChannelCommand(plugin, Channel.HIGHRANK));
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            GlobalChatManager global = GlobalChatManager.getInstance();
            if (global.get(player) != null) global.send(player, message);
            else plugin.getChannelManager().sendChat(player, message);
        });
    }
}
