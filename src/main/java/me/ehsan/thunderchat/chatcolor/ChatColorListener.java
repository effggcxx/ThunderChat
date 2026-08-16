package me.ehsan.thunderchat.chatcolor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ChatColorListener implements Listener {
    private final ChatColorManager manager;
    private final ChatColorCommand command;
    public ChatColorListener(me.ehsan.thunderchat.ThunderChat plugin, ChatColorManager manager, ChatColorCommand command) { this.manager = manager; this.command = command; }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Chat Color")) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!manager.canUse(player)) { player.closeInventory(); return; }
        if (ChatColorCommand.MAIN_TITLE.equals(title)) { if (event.getRawSlot() == 11) command.openColorChoice(player); return; }
        if (ChatColorCommand.CHOICE_TITLE.equals(title)) {
            if (event.getRawSlot() == 11) command.openColors(player);
            else if (event.getRawSlot() == 15) player.sendMessage(Component.text("Gradients are coming soon.", NamedTextColor.GRAY));
            return;
        }
        if (ChatColorCommand.COLORS_TITLE.equals(title)) {
            org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
            String color = colorFromName(clicked);
            if (color == null) return;
            manager.setColor(player, color); player.closeInventory();
            player.sendMessage(Component.text("Chat color applied: " + ChatColorCommand.pretty(color), NamedTextColor.GREEN));
        }
    }
    private String colorFromName(org.bukkit.inventory.ItemStack item) {
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        for (String color : ChatColorCommand.dyes().keySet()) if (ChatColorCommand.pretty(color).equals(name)) return color;
        return null;
    }
    @EventHandler public void onDrag(InventoryDragEvent event) { String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()); if (title.startsWith("Chat Color")) event.setCancelled(true); }
}
