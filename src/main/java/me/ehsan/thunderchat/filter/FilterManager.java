package me.ehsan.thunderchat.filter;

import me.ehsan.thunderchat.ThunderChat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Chat filters: spam, caps, blocked words, and swear words.
 */
public class FilterManager {
    private final ThunderChat plugin;
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> repeatStreak = new ConcurrentHashMap<>();

    public FilterManager(ThunderChat plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getPluginConfig().getBoolean("filter.enabled", true);
    }

    public boolean shouldBlock(Player player, String message) {
        if (!isEnabled()) return false;

        if (player.hasPermission("thunderchat.bypass.filter")
                || player.hasPermission("thunderchat.bypass.spam")) {
            recordMessage(player.getUniqueId(), message);
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

        if (containsSwearWord(message) && !player.hasPermission("thunderchat.bypass.swear")) {
            player.sendMessage(ChatColor.RED + "Please don't use swear words in chat.");
            return true;
        }

        if (containsBlockedWord(message) && !player.hasPermission("thunderchat.bypass.filter")) {
            player.sendMessage(ChatColor.RED + "Your message contains a blocked word.");
            return true;
        }

        recordMessage(player.getUniqueId(), message);
        return false;
    }

    public boolean isSpam(Player player, String message) {
        UUID id = player.getUniqueId();
        String previous = lastMessage.get(id);
        Long previousTime = lastMessageTime.get(id);
        if (previous == null || previousTime == null) return false;

        double similarity = ChatSimilarity.similarity(previous, message);
        long elapsedSec = (System.currentTimeMillis() - previousTime) / 1000L;
        int score = 0;

        if (similarity >= 0.90) score += 2;
        else if (similarity >= 0.80) score += 1;
        if (similarity >= 1.0) score += 2;
        if (elapsedSec < 3) score += 2;
        else if (elapsedSec < 10) score += 1;

        int streak = repeatStreak.getOrDefault(id, 0);
        streak = similarity >= 0.85 ? streak + 1 : 0;
        if (streak >= 3) score += 3;

        int threshold = plugin.getPluginConfig().getInt("filter.spam.score-threshold", 5);
        boolean spam = score >= threshold;
        if (spam) {
            repeatStreak.put(id, streak);
            lastMessageTime.put(id, System.currentTimeMillis());
            return true;
        }

        repeatStreak.put(id, similarity >= 0.85 ? streak : 0);
        return false;
    }

    public boolean isExcessiveCaps(String message) {
        int minLength = plugin.getPluginConfig().getInt("filter.caps.min-length-to-check", 8);
        if (message.length() < minLength) return false;

        int letters = 0;
        int upper = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        if (letters == 0) return false;

        double maxPct = plugin.getPluginConfig().getDouble("filter.caps.max-percentage", 70);
        return ((double) upper / letters) * 100.0 >= maxPct;
    }

    /** Returns true when a configured swear word occurs in the message. */
    public boolean containsSwearWord(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.swear.enabled", true)) return false;

        List<String> swearWords = plugin.getPluginConfig().getStringList("filter.swear.words");
        if (swearWords == null || swearWords.isEmpty()) return false;

        String normalized = normalizeForFilter(message);
        for (String word : swearWords) {
            if (word == null || word.isBlank()) continue;
            String candidate = normalizeForFilter(word).trim();
            if (candidate.isEmpty()) continue;

            // English/ASCII words use boundaries so words such as "class" don't match "ass".
            if (candidate.matches("[a-z0-9_ ]+")) {
                String regex = "(?<![a-z0-9_])" + Pattern.quote(candidate) + "(?![a-z0-9_])";
                if (Pattern.compile(regex).matcher(normalized).find()) return true;
            } else if (normalized.contains(candidate)) {
                // Persian and other non-Latin words are checked as substrings so compounds
                // such as Persian swear-word combinations are still caught.
                return true;
            }
        }
        return false;
    }

    public boolean containsBlockedWord(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.words.enabled", true)) return false;
        List<String> blocked = plugin.getPluginConfig().getStringList("filter.words.blocked");
        if (blocked == null || blocked.isEmpty()) return false;

        String lower = normalizeForFilter(message);
        for (String word : blocked) {
            if (word == null || word.isBlank()) continue;
            if (lower.contains(normalizeForFilter(word).trim())) return true;
        }
        return false;
    }

    private String normalizeForFilter(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('\u200c', ' ')
                .replace('\u200d', ' ')
                .replace('\u064a', '\u06cc')
                .replace('\u0649', '\u06cc')
                .replace('\u0643', '\u06a9')
                .replaceAll("\\s+", " ");
    }

    private void recordMessage(UUID id, String message) {
        lastMessage.put(id, message);
        lastMessageTime.put(id, System.currentTimeMillis());
    }

    public void clear(UUID id) {
        lastMessage.remove(id);
        lastMessageTime.remove(id);
        repeatStreak.remove(id);
    }
}
