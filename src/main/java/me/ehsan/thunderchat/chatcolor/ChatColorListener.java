package me.ehsan.thunderchat.chatcolor;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatColorListener implements Listener {
    private final ThunderChat plugin;
    private final ChatColorManager manager;
    private final ChatColorCommand command;

    public ChatColorListener(ThunderChat plugin, ChatColorManager manager, ChatColorCommand command) {
        this.plugin = plugin; this.manager = manager; this.command = command;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Chat Color")) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!manager.canUse(player)) { player.closeInventory(); return; }
        if (ChatColorCommand.MAIN_TITLE.equals(title)) {
            if (event.getRawSlot() == 11) command.openColorChoice(player);
            return;
        }
        if (ChatColorCommand.CHOICE_TITLE.equals(title)) {
            if (event.getRawSlot() == 11) command.openColors(player);
            else if (event.getRawSlot() == 15) player.sendMessage(Component.text("Gradients are coming soon.", NamedTextColor.GRAY));
            return;
        }
        if (ChatColorCommand.COLORS_TITLE.equals(title)) {
            org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
            String color = colorFromMaterialAndName(clicked);
            if (color == null) return;
            manager.setColor(player, color);
            player.closeInventory();
            player.sendMessage(Component.text("Chat color applied: ", NamedTextColor.GREEN).append(Component.text(ChatColorCommand.pretty(color), NamedTextColor.valueOf(color.equals("dark_aqua") ? "DARK_AQUA" : color.toUpperCase()))));
        }
    }

    private String colorFromMaterialAndName(org.bukkit.inventory.ItemStack item) {
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        for (String color : ChatColorCommand.dyes().keySet()) if (ChatColorCommand.pretty(color).equals(name)) return color;
        return null;
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.startsWith("Chat Color")) event.setCancelled(true);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { /* persisted color intentionally survives restarts */ }
}
