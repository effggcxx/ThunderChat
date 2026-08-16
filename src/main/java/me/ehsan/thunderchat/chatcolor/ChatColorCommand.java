package me.ehsan.thunderchat.chatcolor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class ChatColorCommand implements CommandExecutor {
    public static final String MAIN_TITLE = "Chat Color";
    public static final String CHOICE_TITLE = "Chat Color > Color";
    public static final String COLORS_TITLE = "Chat Color > Colors";
    private final ChatColorManager manager;

    private static final Map<String, Material> DYES = Map.ofEntries(
            Map.entry("black", Material.BLACK_DYE), Map.entry("dark_blue", Material.BLUE_DYE),
            Map.entry("dark_green", Material.GREEN_DYE), Map.entry("dark_aqua", Material.CYAN_DYE),
            Map.entry("dark_red", Material.RED_DYE), Map.entry("dark_purple", Material.PURPLE_DYE),
            Map.entry("gold", Material.ORANGE_DYE), Map.entry("gray", Material.LIGHT_GRAY_DYE),
            Map.entry("dark_gray", Material.GRAY_DYE), Map.entry("blue", Material.BLUE_DYE),
            Map.entry("green", Material.LIME_DYE), Map.entry("aqua", Material.CYAN_DYE),
            Map.entry("red", Material.RED_DYE), Map.entry("light_purple", Material.PINK_DYE),
            Map.entry("yellow", Material.YELLOW_DYE), Map.entry("white", Material.WHITE_DYE)
    );

    public ChatColorCommand(ChatColorManager manager) { this.manager = manager; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players can use this command."); return true; }
        if (!manager.canUse(player)) { player.sendMessage(Component.text("You don't have permission to use Chat Color.", NamedTextColor.RED)); return true; }
        openMain(player);
        return true;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(MAIN_TITLE));
        inv.setItem(11, item(Material.RED_DYE, "Color", "Choose a color for your chat."));
        inv.setItem(13, item(Material.NETHER_STAR, "Style", "Coming soon."));
        inv.setItem(15, item(Material.NAME_TAG, "Custom", "Coming soon."));
        player.openInventory(inv);
    }

    public void openColorChoice(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(CHOICE_TITLE));
        inv.setItem(11, item(Material.RED_DYE, "Colors", "Choose a single chat color."));
        ItemStack gradient = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = gradient.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<rainbow>Gradients</rainbow>"));
        meta.lore(java.util.List.of(Component.text("Coming soon.", NamedTextColor.GRAY)));
        gradient.setItemMeta(meta);
        inv.setItem(15, gradient);
        player.openInventory(inv);
    }

    public void openColors(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(COLORS_TITLE));
        int slot = 10;
        for (Map.Entry<String, Material> entry : DYES.entrySet()) {
            inv.setItem(slot, coloredItem(entry.getKey(), entry.getValue()));
            slot++;
            if (slot % 9 == 17) slot += 2;
        }
        player.openInventory(inv);
    }

    private ItemStack coloredItem(String color, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<" + color + ">" + pretty(color) + "</" + color + ">"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE));
        meta.lore(java.util.List.of(Component.text(lore, NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    public static String pretty(String id) { String[] parts = id.split("_"); StringBuilder out = new StringBuilder(); for (String part : parts) { if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' '); } return out.toString().trim(); }
    public static Map<String, Material> dyes() { return DYES; }
}
