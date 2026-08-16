package me.ehsan.thunderchat.chatcolor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ChatColorCommand implements CommandExecutor {
    public static final String MAIN_TITLE = "Chat Color";
    public static final String CHOICE_TITLE = "Chat Color > Color";
    public static final String COLORS_TITLE = "Chat Color > Colors";
    public static final String GRADIENTS_TITLE = "Chat Color > Gradients";
    public static final String STYLES_TITLE = "Chat Color > Styles";
    private final ChatColorManager manager;
    private static final String[] SLOTS = {"10","11","12","13","14","15","16","17","19","20","21","22","23","24","25","26"};
    private static final Map<String, Material> DYES = Map.ofEntries(
            Map.entry("black", Material.BLACK_DYE), Map.entry("dark_blue", Material.BLUE_DYE), Map.entry("dark_green", Material.GREEN_DYE), Map.entry("dark_aqua", Material.CYAN_DYE),
            Map.entry("dark_red", Material.RED_DYE), Map.entry("dark_purple", Material.PURPLE_DYE), Map.entry("gold", Material.ORANGE_DYE), Map.entry("gray", Material.LIGHT_GRAY_DYE),
            Map.entry("dark_gray", Material.GRAY_DYE), Map.entry("blue", Material.BLUE_DYE), Map.entry("green", Material.LIME_DYE), Map.entry("aqua", Material.CYAN_DYE),
            Map.entry("red", Material.RED_DYE), Map.entry("light_purple", Material.PINK_DYE), Map.entry("yellow", Material.YELLOW_DYE), Map.entry("white", Material.WHITE_DYE)
    );
    private static final Map<String, Material> GRADIENT_ICONS = Map.of("sunset", Material.ORANGE_DYE, "ocean", Material.PRISMARINE_CRYSTALS, "forest", Material.OAK_LEAVES, "fire", Material.BLAZE_POWDER, "candy", Material.PINK_DYE, "aurora", Material.AMETHYST_SHARD, "rainbow", Material.FIREWORK_STAR);
    private static final Map<String, Material> STYLE_ICONS = Map.of("bold", Material.ANVIL, "italic", Material.WRITABLE_BOOK, "underlined", Material.PAPER, "strikethrough", Material.IRON_SWORD);

    public ChatColorCommand(ChatColorManager manager) { this.manager = manager; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players can use this command."); return true; }
        if (!manager.canUse(player)) { player.sendMessage(Component.text("You don't have permission to use Chat Color.", NamedTextColor.RED)); return true; }
        openMain(player); return true;
    }
    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(MAIN_TITLE));
        inv.setItem(11, item(Material.RED_DYE, "Color", "Choose a color or gradient for your chat."));
        inv.setItem(13, item(Material.NETHER_STAR, "Style", "Choose one or more chat styles."));
        inv.setItem(15, item(Material.NAME_TAG, "Custom", "Coming soon.")); player.openInventory(inv);
    }
    public void openColorChoice(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(CHOICE_TITLE));
        inv.setItem(11, item(Material.RED_DYE, "Colors", "Choose a single chat color."));
        ItemStack gradient = new ItemStack(Material.FIREWORK_STAR); ItemMeta meta = gradient.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<rainbow>Gradients</rainbow>")); meta.lore(List.of(Component.text("Choose a gradient for your chat.", NamedTextColor.GRAY))); gradient.setItemMeta(meta);
        inv.setItem(15, gradient); player.openInventory(inv);
    }
    public void openColors(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(COLORS_TITLE));
        int i = 0; for (Map.Entry<String, Material> entry : DYES.entrySet()) { String color = entry.getKey(); inv.setItem(Integer.parseInt(SLOTS[i++]), manager.canUseColor(player, color) ? coloredItem(color, entry.getValue()) : lockedItem(entry.getValue(), pretty(color), "No permission.")); }
        player.openInventory(inv);
    }
    public void openGradients(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(GRADIENTS_TITLE));
        int i = 0; for (String gradient : ChatColorManager.GRADIENTS) { int slot = Integer.parseInt(SLOTS[i++]); if (manager.canUseGradient(player, gradient)) inv.setItem(slot, gradientItem(gradient, GRADIENT_ICONS.get(gradient))); else inv.setItem(slot, lockedItem(GRADIENT_ICONS.get(gradient), pretty(gradient), "No permission.")); }
        player.openInventory(inv);
    }
    public void openStyles(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(STYLES_TITLE));
        String[] slots = {"11", "12", "14", "15"}; int i = 0;
        for (String style : ChatColorManager.STYLES) { int slot = Integer.parseInt(slots[i++]); if (manager.canUseStyle(player, style)) inv.setItem(slot, styleItem(style)); else inv.setItem(slot, lockedItem(STYLE_ICONS.get(style), pretty(style), "No permission.")); }
        player.openInventory(inv);
    }
    private ItemStack coloredItem(String color, Material material) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.displayName(MiniMessage.miniMessage().deserialize("<" + color + ">" + pretty(color) + "</" + color + ">")); meta.lore(List.of(Component.text("Click to apply.", NamedTextColor.GRAY))); item.setItemMeta(meta); return item; }
    private ItemStack gradientItem(String gradient, Material material) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); String tag = manager.gradientTag(gradient); String close = "rainbow".equals(tag) ? "</rainbow>" : "</gradient>"; meta.displayName(MiniMessage.miniMessage().deserialize("<" + tag + ">" + pretty(gradient) + close)); meta.lore(List.of(Component.text("Click to apply and replace your current color.", NamedTextColor.GRAY))); item.setItemMeta(meta); return item; }
    private ItemStack styleItem(String style) { ItemStack item = new ItemStack(STYLE_ICONS.get(style)); ItemMeta meta = item.getItemMeta(); boolean selected = manager.hasStyle(playerForMeta(meta), style); meta.displayName(Component.text(pretty(style), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE)); meta.lore(List.of(Component.text(selected ? "Enabled - click to disable." : "Disabled - click to enable.", NamedTextColor.GRAY))); item.setItemMeta(meta); return item; }
    private Player playerForMeta(ItemMeta ignored) { return null; }
    private ItemStack lockedItem(Material material, String name, String lore) { return item(material == null ? Material.BARRIER : material, name, lore); }
    private ItemStack item(Material material, String name, String lore) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.displayName(Component.text(name, NamedTextColor.WHITE)); meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY))); item.setItemMeta(meta); return item; }
    public static String pretty(String id) { String[] parts = id.split("_"); StringBuilder out = new StringBuilder(); for (String part : parts) if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' '); return out.toString().trim(); }
    public static Map<String, Material> dyes() { return DYES; }
    public static Map<String, Material> gradientIcons() { return GRADIENT_ICONS; }
    public static Map<String, Material> styleIcons() { return STYLE_ICONS; }
}