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
 * Chat filters: spam, flood, caps, blocked words, swear words, and advertisements.
 */
public class FilterManager {
    private final ThunderChat plugin;
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> repeatStreak = new ConcurrentHashMap<>();

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?(?![0-9])");
    private static final Pattern SERVER_DOMAIN_PATTERN = Pattern.compile(
            "(?i)(?<![a-z0-9_-])(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+(?:[a-z]{2,63}|xn--[a-z0-9-]{2,59})(?::[0-9]{1,5})?(?![a-z0-9_-])");

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

        if (isFlood(message) && !player.hasPermission("thunderchat.bypass.flood")) {
            player.sendMessage(ChatColor.RED + "Please don't flood the chat with repeated characters.");
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

        if (containsAdvertisement(message) && !player.hasPermission("thunderchat.bypass.advertisement")) {
            player.sendMessage(ChatColor.RED + "Please don't advertise other Minecraft servers in chat.");
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

    public boolean isFlood(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.flood.enabled", true)) return false;
        if (message == null || message.isEmpty()) return false;

        int threshold = plugin.getPluginConfig().getInt("filter.flood.max-consecutive-characters", 3);
        threshold = Math.max(2, threshold);

        int run = 1;
        char previous = message.charAt(0);
        for (int i = 1; i < message.length(); i++) {
            char current = message.charAt(i);
            if (current == previous) {
                run++;
                if (run >= threshold) return true;
            } else {
                previous = current;
                run = 1;
            }
        }
        return false;
    }

    public boolean isExcessiveCaps(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.caps.enabled", true)) return false;
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

    public boolean containsSwearWord(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.swear.enabled", true)) return false;

        List<String> swearWords = plugin.getPluginConfig().getStringList("filter.swear.words");
        if (swearWords == null || swearWords.isEmpty()) return false;

        String normalized = normalizeForFilter(message);
        for (String word : swearWords) {
            if (word == null || word.isBlank()) continue;
            String candidate = normalizeForFilter(word).trim();
            if (candidate.isEmpty()) continue;

            if (candidate.matches("[a-z0-9_ ]+")) {
                String regex = "(?<![a-z0-9_])" + Pattern.quote(candidate) + "(?![a-z0-9_])";
                if (Pattern.compile(regex).matcher(normalized).find()) return true;
            } else if (normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Blocks common Minecraft server advertisements. Detection is intentionally
     * conservative around domains: it catches numeric IPv4 addresses, domain
     * names with a real TLD, and configurable server names from the config.
     */
    public boolean containsAdvertisement(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.advertisement.enabled", true)) return false;
        if (message == null || message.isBlank()) return false;

        String normalized = normalizeForFilter(message);

        if (plugin.getPluginConfig().getBoolean("filter.advertisement.block-ip", true)
                && IPV4_PATTERN.matcher(normalized).find()) {
            return true;
        }

        if (plugin.getPluginConfig().getBoolean("filter.advertisement.block-domains", true)
                && SERVER_DOMAIN_PATTERN.matcher(normalized).find()) {
            return true;
        }

        List<String> serverNames = plugin.getPluginConfig().getStringList("filter.advertisement.server-names");
        for (String name : serverNames) {
            if (name == null || name.isBlank()) continue;
            String candidate = normalizeForFilter(name).trim();
            if (!candidate.isEmpty() && normalized.contains(candidate)) return true;
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
