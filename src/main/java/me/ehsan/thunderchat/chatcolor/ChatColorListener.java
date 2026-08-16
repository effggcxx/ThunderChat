package me.ehsan.thunderchat.chatcolor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class ChatColorListener implements Listener {
    private final ChatColorManager manager;
    private final ChatColorCommand command;
    public ChatColorListener(me.ehsan.thunderchat.ThunderChat plugin, ChatColorManager manager, ChatColorCommand command) { this.manager = manager; this.command = command; }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Chat Color")) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!manager.canUse(player)) { player.closeInventory(); return; }

        if (ChatColorCommand.MAIN_TITLE.equals(title)) {
            if (event.getRawSlot() == 11) command.openColorChoice(player);
            else if (event.getRawSlot() == 13) command.openStyles(player);
            return;
        }
        if (ChatColorCommand.CHOICE_TITLE.equals(title)) {
            if (event.getRawSlot() == 11) command.openColors(player);
            else if (event.getRawSlot() == 15) command.openGradients(player);
            return;
        }
        if (ChatColorCommand.COLORS_TITLE.equals(title)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
            String color = colorFromName(clicked);
            if (color == null || !manager.canUseColor(player, color)) return;
            manager.setColor(player, color); player.closeInventory();
            player.sendMessage(Component.text("Chat color applied: " + ChatColorCommand.pretty(color), NamedTextColor.GREEN));
            return;
        }
        if (ChatColorCommand.GRADIENTS_TITLE.equals(title)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
            String gradient = gradientFromName(clicked);
            if (gradient == null || !manager.canUseGradient(player, gradient)) return;
            manager.setGradient(player, gradient); player.closeInventory();
            player.sendMessage(Component.text("Chat gradient applied: " + ChatColorCommand.pretty(gradient), NamedTextColor.GREEN));
            return;
        }
        if (ChatColorCommand.STYLES_TITLE.equals(title)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
            String style = styleFromName(clicked);
            if (style == null || !manager.canUseStyle(player, style)) return;
            manager.toggleStyle(player, style);
            command.openStyles(player);
        }
    }
    private String colorFromName(ItemStack item) {
        String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        for (String color : ChatColorCommand.dyes().keySet()) if (ChatColorCommand.pretty(color).equals(name)) return color;
        return null;
    }
    private String gradientFromName(ItemStack item) {
        String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        for (String gradient : ChatColorManager.GRADIENTS) if (ChatColorCommand.pretty(gradient).equals(name)) return gradient;
        return null;
    }
    private String styleFromName(ItemStack item) {
        String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        for (String style : ChatColorManager.STYLES) if (ChatColorCommand.pretty(style).equals(name)) return style;
        return null;
    }
    @EventHandler public void onDrag(InventoryDragEvent event) { String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title()); if (title.startsWith("Chat Color")) event.setCancelled(true); }
}
