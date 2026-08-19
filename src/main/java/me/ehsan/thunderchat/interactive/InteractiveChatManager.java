package me.ehsan.thunderchat.interactive;

import me.ehsan.thunderchat.ThunderChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;
import java.util.UUID;

/** Handles lightweight InteractiveChat-style [item]/[i] and [inv] placeholders. */
public final class InteractiveChatManager {
    private final ThunderChat plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public InteractiveChatManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public Component decorate(Player sender, String renderedMessage) {
        Component component = legacy.deserialize(renderedMessage);
        if (sender.hasPermission("thunderchat.interactive.item")) {
            Component item = itemComponent(sender);
            component = component.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                    .matchLiteral("[item]").replacement(item).build());
            component = component.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                    .matchLiteral("[i]").replacement(item).build());
        }
        if (sender.hasPermission("thunderchat.interactive.inventory")) {
            Component inventory = inventoryComponent(sender);
            component = component.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                    .matchLiteral("[inv]").replacement(inventory).build());
        }
        return component;
    }

    private Component itemComponent(Player sender) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return plugin.getMessagesManager().get("interactive.item-empty", "<gray>[No item]</gray>");
        }
        String name = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                ? legacy.serialize(stack.displayName())
                : prettyMaterial(stack.getType());
        String details = plugin.getMessagesManager().raw("interactive.item-hover",
                "<yellow>{item}</yellow><gray> x{amount}</gray>");
        details = details.replace("{item}", name).replace("{amount}", Integer.toString(stack.getAmount()));
        Component hover = miniMessage.deserialize(details);
        return plugin.getMessagesManager().get("interactive.item-text", "<aqua>[Item]</aqua>")
                .hoverEvent(HoverEvent.showText(hover));
    }

    private Component inventoryComponent(Player sender) {
        Component hover = plugin.getMessagesManager().get("interactive.inventory-hover",
                "<yellow>Click to view <white>{player}</white>'s inventory.</yellow>",
                java.util.Map.of("player", sender.getName()));
        return plugin.getMessagesManager().get("interactive.inventory-text", "<aqua>[Inventory]</aqua>")
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.runCommand("/thunderchat inventory " + sender.getUniqueId()));
    }

    public boolean openInventory(Player viewer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return false;
        Inventory source = target.getInventory();
        Inventory view = Bukkit.createInventory(null, 54,
                plugin.getMessagesManager().parse("<dark_gray>{player}'s Inventory",
                        java.util.Map.of("player", target.getName())));
        view.setContents(new ItemStack[54]);
        PlayerInventory targetInventory = target.getInventory();
        for (int slot = 0; slot < 36; slot++) view.setItem(slot, clone(targetInventory.getItem(slot)));
        view.setItem(45, clone(targetInventory.getHelmet()));
        view.setItem(46, clone(targetInventory.getChestplate()));
        view.setItem(47, clone(targetInventory.getLeggings()));
        view.setItem(48, clone(targetInventory.getBoots()));
        view.setItem(49, clone(targetInventory.getItemInOffHand()));
        viewer.openInventory(view);
        return true;
    }

    private ItemStack clone(ItemStack stack) { return stack == null ? null : stack.clone(); }

    private String prettyMaterial(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
