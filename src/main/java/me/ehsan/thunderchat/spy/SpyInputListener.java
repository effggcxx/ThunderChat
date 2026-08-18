package me.ehsan.thunderchat.spy;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.AnvilInventory;

import java.util.StringJoiner;

/** Captures local player input in anvils, signs and books for the local spy system. */
public final class SpyInputListener implements Listener {
    private final ThunderChat plugin;

    public SpyInputListener(ThunderChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilRename(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && event.getInventory().getType() == InventoryType.ANVIL
                && event.getRawSlot() == 2
                && event.getInventory() instanceof AnvilInventory anvil) {
            String rename = anvil.getRenameText();
            if (rename != null && !rename.isBlank()) plugin.getSpyManager().spyAnvil(player, rename);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        StringJoiner lines = new StringJoiner(" | ");
        for (String line : event.getLines()) {
            if (line != null && !line.isEmpty()) lines.add(line);
        }
        String text = lines.toString();
        if (!text.isBlank()) plugin.getSpyManager().spySign(player, text);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.getNewBookMeta() == null || !event.getNewBookMeta().hasPages()) return;
        StringJoiner pages = new StringJoiner(" | ");
        event.getNewBookMeta().getPages().forEach(page -> {
            if (page != null && !page.isBlank()) pages.add(page);
        });
        String text = pages.toString();
        if (!text.isBlank()) plugin.getSpyManager().spyBook(event.getPlayer(), text);
    }
}
