package me.ehsan.thunderchat.filter;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Handles the anti-caps chat rule. */
public final class CapsManager {
    private final ThunderChat plugin;

    public CapsManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true when the message is considered all caps.
     * Non-letter characters are ignored; at least the configured minimum
     * number of letters must be present.
     */
    public boolean isAllCaps(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.caps.enabled", true)) {
            return false;
        }

        int minLength = plugin.getPluginConfig().getInt("filter.caps.min-length-to-check", 8);
        int letters = 0;
        int uppercase = 0;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    uppercase++;
                }
            }
        }

        return letters >= minLength && uppercase == letters;
    }

    public String normalize(String message) {
        return message.toLowerCase(Locale.ROOT);
    }

    public void notifyPlayer(Player player) {
        player.sendMessage("§cPlease don't write in all caps.");
    }

    public boolean canBypass(Player player) {
        return player.hasPermission("thunderchat.bypass.caps");
    }
}
