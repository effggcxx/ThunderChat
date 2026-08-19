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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InteractiveChat-inspired placeholder engine adapted for ThunderChat/Paper.
 *
 * This implementation is intentionally native to Paper and does not require
 * InteractiveChat itself. It provides item, inventory, ender chest, player
 * name interaction, command interaction, player information placeholders,
 * configurable custom placeholders, cooldowns and a placeholder limit.
 */
public final class InteractiveChatManager {
    private final ThunderChat plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Pattern PLAYER_TOKEN = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})(?![A-Za-z0-9_])");

    public InteractiveChatManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public Component decorate(Player sender, String renderedMessage) {
        Component component = legacy.deserialize(renderedMessage);
        int limit = plugin.getPluginConfig().getInt("interactive.max-placeholders", 12);
        int used = 0;

        if (sender.hasPermission("thunderchat.interactive.item") || sender.hasPermission("thunderchat.interactive.*")) {
            Component item = itemComponent(sender);
            component = replace(component, "[item]", item);
            component = replace(component, "[i]", item);
            used += count(renderedMessage, "[item]") + count(renderedMessage, "[i]");
        }
        if (sender.hasPermission("thunderchat.interactive.inventory") || sender.hasPermission("thunderchat.interactive.*")) {
            Component inventory = inventoryComponent(sender);
            component = replace(component, "[inv]", inventory);
            component = replace(component, "[inventory]", inventory);
            used += countIgnoreCase(renderedMessage, "[inv]") + countIgnoreCase(renderedMessage, "[inventory]");
        }
        if (sender.hasPermission("thunderchat.interactive.ender") || sender.hasPermission("thunderchat.interactive.*")) {
            Component ender = enderComponent(sender);
            component = replace(component, "[ender]", ender);
            component = replace(component, "[e]", ender);
            used += countIgnoreCase(renderedMessage, "[ender]") + countIgnoreCase(renderedMessage, "[e]");
        }
        if (sender.hasPermission("thunderchat.interactive.position") || sender.hasPermission("thunderchat.interactive.*")) {
            component = replace(component, "[pos]", infoComponent("Position", position(sender), null));
            used += countIgnoreCase(renderedMessage, "[pos]");
        }
        if (sender.hasPermission("thunderchat.interactive.ping") || sender.hasPermission("thunderchat.interactive.*")) {
            component = replace(component, "[ping]", infoComponent("Ping", sender.getPing() + "ms", null));
            used += countIgnoreCase(renderedMessage, "[ping]");
        }
        if (sender.hasPermission("thunderchat.interactive.player") || sender.hasPermission("thunderchat.interactive.*")) {
            component = decoratePlayerNames(component, sender);
        }
        if (sender.hasPermission("thunderchat.interactive.commands") || sender.hasPermission("thunderchat.interactive.*")) {
            component = decorateCommands(component);
        }
        if (sender.hasPermission("thunderchat.interactive.custom") || sender.hasPermission("thunderchat.interactive.custom.*") || sender.hasPermission("thunderchat.interactive.*")) {
            component = applyCustomPlaceholders(component, sender);
        }

        if (limit >= 0 && used > limit) {
            plugin.getMessagesManager().send(sender, "interactive.limit-reached", "<red>Please do not use an excessive amount of interactive placeholders.");
            return legacy.deserialize(renderedMessage);
        }
        return component;
    }

    private Component itemComponent(Player sender) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return plugin.getMessagesManager().get("interactive.item-empty", "<gray>[No item]</gray>");
        }
        String itemName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                ? legacy.serialize(stack.displayName()) : prettyMaterial(stack.getType());
        String hoverText = plugin.getMessagesManager().raw("interactive.item-hover", "<yellow>{item}</yellow> <gray>x{amount}</gray>")
                .replace("{item}", itemName).replace("{amount}", Integer.toString(stack.getAmount()));
        Component result = plugin.getMessagesManager().get("interactive.item-text", "<aqua>[Item]</aqua>")
                .hoverEvent(HoverEvent.showText(miniMessage.deserialize(hoverText)));
        if (plugin.getPluginConfig().getBoolean("interactive.item.click-enabled", true)) {
            result = result.clickEvent(ClickEvent.runCommand("/thunderchat item " + sender.getUniqueId()));
        }
        return result;
    }

    private Component inventoryComponent(Player sender) {
        Component result = plugin.getMessagesManager().get("interactive.inventory-text", "<aqua>[Inventory]</aqua>")
                .hoverEvent(HoverEvent.showText(plugin.getMessagesManager().get("interactive.inventory-hover",
                        "<yellow>Click to view <white>{player}</white>'s inventory.</yellow>", Map.of("player", sender.getName()))));
        return result.clickEvent(ClickEvent.runCommand("/thunderchat inventory " + sender.getUniqueId()));
    }

    private Component enderComponent(Player sender) {
        Component result = plugin.getMessagesManager().get("interactive.ender-text", "<light_purple>[Ender Chest]</light_purple>")
                .hoverEvent(HoverEvent.showText(plugin.getMessagesManager().get("interactive.ender-hover",
                        "<yellow>Click to view <white>{player}</white>'s Ender Chest.</yellow>", Map.of("player", sender.getName()))));
        return result.clickEvent(ClickEvent.runCommand("/thunderchat ender " + sender.getUniqueId()));
    }

    private Component infoComponent(String title, String value, ClickEvent click) {
        Component result = Component.text("[" + title + "]", NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(Component.text(value)));
        return click == null ? result : result.clickEvent(click);
    }

    private Component decoratePlayerNames(Component source, Player sender) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(sender.getUniqueId())) continue;
            String name = target.getName();
            String hover = plugin.getMessagesManager().raw("interactive.player-hover",
                    "<gray>World: <white>{world}</white>\n<gray>Ping: <white>{ping}ms</white>\n<gray>Health: <white>{health}</white>")
                    .replace("{world}", target.getWorld().getName())
                    .replace("{ping}", Integer.toString(target.getPing()))
                    .replace("{health}", Integer.toString((int) Math.ceil(target.getHealth())));
            Component replacement = Component.text(name)
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize(hover)))
                    .clickEvent(ClickEvent.suggestCommand("/msg " + name + " "));
            source = source.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                    .match("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])")
                    .replacement(replacement).build());
        }
        return source;
    }

    private Component decorateCommands(Component source) {
        String format = plugin.getPluginConfig().getString("interactive.commands.format", "[{command}]");
        String hover = plugin.getPluginConfig().getString("interactive.commands.hover", "<yellow>Click to use command</yellow>");
        Pattern pattern = Pattern.compile("(?<!\\S)(/\\S+)");
        String serialized = legacy.serialize(source);
        Matcher matcher = pattern.matcher(serialized);
        Component result = Component.empty();
        int last = 0;
        while (matcher.find()) {
            result = result.append(legacy.deserialize(serialized.substring(last, matcher.start())));
            String command = matcher.group(1);
            String visible = format.replace("{command}", command);
            result = result.append(miniMessage.deserialize(visible)
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize(hover)))
                    .clickEvent(ClickEvent.suggestCommand(command)));
            last = matcher.end();
        }
        return last == 0 ? source : result.append(legacy.deserialize(serialized.substring(last)));
    }

    private Component applyCustomPlaceholders(Component source, Player sender) {
        FileConfiguration cfg = plugin.getPluginConfig();
        ConfigurationSection section = cfg.getConfigurationSection("interactive.custom-placeholders");
        if (section == null) return source;
        Component result = source;
        for (String id : section.getKeys(false)) {
            String path = "interactive.custom-placeholders." + id;
            String regex = cfg.getString(path + ".keyword", "");
            if (regex.isEmpty()) continue;
            if (!sender.hasPermission("thunderchat.interactive.custom." + id) && !sender.hasPermission("thunderchat.interactive.custom.*") && !sender.hasPermission("thunderchat.interactive.*")) continue;
            if (!ready(sender, id, cfg.getInt(path + ".cooldown", 0))) continue;
            String replace = cfg.getString(path + ".replace", "");
            if (!replace.isEmpty()) {
                String parsed = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")
                        ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(sender, replace) : replace;
                result = result.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().match(regex).replacement(legacy.deserialize(parsed)).build());
            }
        }
        return result;
    }

    private boolean ready(Player player, String key, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        Long previous = playerCooldowns.get(key);
        if (previous != null && now - previous < cooldownSeconds * 1000L) return false;
        playerCooldowns.put(key, now);
        return true;
    }

    public boolean openInventory(Player viewer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return false;
        PlayerInventory source = target.getInventory();
        ReadOnlyHolder holder = new ReadOnlyHolder();
        Inventory view = Bukkit.createInventory(holder, 54,
                plugin.getMessagesManager().parse("<dark_gray>{player}'s Inventory", Map.of("player", target.getName())));
        holder.setInventory(view);
        for (int slot = 0; slot < 36; slot++) view.setItem(slot, clone(source.getItem(slot)));
        view.setItem(45, clone(source.getHelmet()));
        view.setItem(46, clone(source.getChestplate()));
        view.setItem(47, clone(source.getLeggings()));
        view.setItem(48, clone(source.getBoots()));
        view.setItem(49, clone(source.getItemInOffHand()));
        viewer.openInventory(view);
        return true;
    }

    public boolean openEnderChest(Player viewer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return false;
        ReadOnlyHolder holder = new ReadOnlyHolder();
        Inventory view = Bukkit.createInventory(holder, 27,
                plugin.getMessagesManager().parse("<dark_purple>{player}'s Ender Chest", Map.of("player", target.getName())));
        for (int slot = 0; slot < 27; slot++) view.setItem(slot, clone(target.getEnderChest().getItem(slot)));
        holder.setInventory(view);
        viewer.openInventory(view);
        return true;
    }

    public boolean openItem(Player viewer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return false;
        ReadOnlyHolder holder = new ReadOnlyHolder();
        Inventory view = Bukkit.createInventory(holder, 27,
                plugin.getMessagesManager().parse("<dark_gray>{player}'s Item", Map.of("player", target.getName())));
        holder.setInventory(view);
        view.setItem(13, clone(target.getInventory().getItemInMainHand()));
        viewer.openInventory(view);
        return true;
    }

    public boolean isReadOnly(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ReadOnlyHolder;
    }

    private ItemStack clone(ItemStack stack) { return stack == null ? null : stack.clone(); }

    private Component replace(Component source, String token, Component replacement) {
        return source.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(token).replacement(replacement).build());
    }

    private int count(String value, String token) { return value.split(Pattern.quote(token), -1).length - 1; }
    private int countIgnoreCase(String value, String token) { return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(value).results().toList().size(); }
    private String position(Player player) { return player.getWorld().getName() + " " + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ(); }
    private String prettyMaterial(Material material) { String[] words = material.name().toLowerCase(Locale.ROOT).split("_"); StringBuilder result = new StringBuilder(); for (String word : words) { if (word.isEmpty()) continue; if (result.length() > 0) result.append(' '); result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); } return result.toString(); }

    private static final class ReadOnlyHolder implements InventoryHolder {
        private Inventory inventory;
        void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
