package me.ehsan.thunderchat.filter;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat filters: spam (similarity + timing score), caps, blocked words.
 * Spam uses Jaccard/LCS similarity against the player's last message only —
 * cheap enough to run on every chat message on a busy Paper server.
 */
public class FilterManager {

    private final ThunderChat plugin;

    /** Last chat message per player (raw text). */
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();

    /** Epoch millis of last chat message per player. */
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    /** How many times in a row the player sent a highly similar message. */
    private final Map<UUID, Integer> repeatStreak = new ConcurrentHashMap<>();

    public FilterManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getPluginConfig().getBoolean("filter.enabled", true);
    }

    /**
     * Full filter pipeline. Returns {@code true} if the message should be blocked.
     * On block, the player is notified in chat.
     */
    public boolean shouldBlock(Player player, String message) {
        if (!isEnabled()) {
            return false;
        }

        if (player.hasPermission("thunderchat.bypass.filter")
                || player.hasPermission("thunderchat.bypass.spam")) {
            lastMessage.put(player.getUniqueId(), message);
            lastMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
            return false;
        }

        if (isSpam(player, message)) {
            player.sendMessage(ChatColor.RED + "Please don't spam the chat.");
            return true;
        }

        if (isExcessiveCaps(message) && !player.hasPermission("thunderchat.bypass.filter")) {
            player.sendMessage(ChatColor.RED + "Please don't use excessive caps.");
            return true;
        }

        if (containsBlockedWord(message) && !player.hasPermission("thunderchat.bypass.filter")) {
            player.sendMessage(ChatColor.RED + "Your message contains a blocked word.");
            return true;
        }

        lastMessage.put(player.getUniqueId(), message);
        lastMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
        return false;
    }

    /**
     * Spam score:
     * Similarity ≥ 90% → +2
     * Similarity ≥ 80% → +1
     * Exact same (sim 1.0) → +2
     * Sent &lt; 3s after previous → +2
     * Sent &lt; 10s after previous → +1
     * 3+ similar repetitions in a row → +3
     * Score ≥ threshold (default 5) → spam.
     */
    public boolean isSpam(Player player, String message) {
        UUID id = player.getUniqueId();
        String previous = lastMessage.get(id);
        Long previousTime = lastMessageTime.get(id);

        if (previous == null || previousTime == null) {
            return false;
        }

        double similarity = ChatSimilarity.similarity(previous, message);
        long elapsedSec = (System.currentTimeMillis() - previousTime) / 1000L;

        int score = 0;

        if (similarity >= 0.90) {
            score += 2;
        } else if (similarity >= 0.80) {
            score += 1;
        }

        if (similarity >= 1.0) {
            score += 2;
        }

        if (elapsedSec < 3) {
            score += 2;
        } else if (elapsedSec < 10) {
            score += 1;
        }

        int streak = repeatStreak.getOrDefault(id, 0);
        if (similarity >= 0.85) {
            streak = streak + 1;
        } else {
            streak = 0;
        }

        if (streak >= 3) {
            score += 3;
        }

        int spamThreshold = plugin.getPluginConfig().getInt("filter.spam.score-threshold", 5);
        boolean spam = score >= spamThreshold;

        if (spam) {
            repeatStreak.put(id, streak);
            lastMessageTime.put(id, System.currentTimeMillis());
            return true;
        }

        if (similarity >= 0.85) {
            repeatStreak.put(id, streak);
        } else {
            repeatStreak.put(id, 0);
        }

        return false;
    }

    public boolean isExcessiveCaps(String message) {
        int minLength = plugin.getPluginConfig().getInt("filter.caps.min-length-to-check", 8);
        if (message.length() < minLength) {
            return false;
        }

        int letters = 0;
        int upper = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }

        if (letters == 0) {
            return false;
        }

        double maxPct = plugin.getPluginConfig().getDouble("filter.caps.max-percentage", 70);
        return ((double) upper / letters) * 100.0 >= maxPct;
    }

    public boolean containsBlockedWord(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.words.enabled", true)) {
            return false;
        }

        java.util.List<String> blocked = plugin.getPluginConfig().getStringList("filter.words.blocked");
        if (blocked == null || blocked.isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase(java.util.Locale.ROOT);
        for (String word : blocked) {
            if (word == null || word.isBlank()) continue;
            if (lower.contains(word.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void recordMessage(UUID id, String message, boolean resetStreakIfDissimilar) {
        lastMessage.put(id, message);
        lastMessageTime.put(id, System.currentTimeMillis());
    }

    /** Clear tracking for a player (e.g. on quit). */
    public void clear(UUID id) {
        lastMessage.remove(id);
        lastMessageTime.remove(id);
        repeatStreak.remove(id);
    }
}