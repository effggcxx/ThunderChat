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

/** Chat filters: spam, flood, caps normalization, blocked words, swear words, and advertisements. */
public class FilterManager {
    private final ThunderChat plugin;
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> repeatStreak = new ConcurrentHashMap<>();

    private volatile int swearConfigHash;
    private volatile List<Pattern> cachedLatinSwears = List.of();
    private volatile List<String> cachedOtherSwears = List.of();

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?(?![0-9])");
    private static final Pattern SERVER_DOMAIN_PATTERN = Pattern.compile(
            "(?i)(?<![a-z0-9_-])(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+(?:[a-z]{2,63}|xn--[a-z0-9-]{2,59})(?::[0-9]{1,5})?(?![a-z0-9_-])");

    public FilterManager(ThunderChat plugin) { this.plugin = plugin; }
    public boolean isEnabled() { return plugin.getPluginConfig().getBoolean("filter.enabled", true); }

    public boolean shouldBlock(Player player, String message) { return shouldBlock(player, message, false); }
    public boolean shouldBlockPrivateMessage(Player player, String message) { return shouldBlock(player, message, true); }

    private boolean shouldBlock(Player player, String message, boolean privateMessage) {
        if (!isEnabled()) return false;
        if (privateMessage && !plugin.getPluginConfig().getBoolean("filter.private-messages.enabled", true)) return false;
        boolean bypassAll = player.hasPermission("thunderchat.bypass.filter");
        boolean bypassSpam = bypassAll || player.hasPermission("thunderchat.bypass.spam");
        if (bypassAll) { recordMessage(player.getUniqueId(), message); return false; }

        if (!bypassSpam && isSpam(player, message)) {
            player.sendMessage(ChatColor.RED + "Please don't spam the chat.");
            plugin.getAlertManager().alert("spam", player, message); return true;
        }
        if (isFlood(message) && !player.hasPermission("thunderchat.bypass.flood")) {
            player.sendMessage(ChatColor.RED + "Please don't flood the chat with repeated characters.");
            plugin.getAlertManager().alert("flood", player, message); return true;
        }
        if (containsSwearWord(message) && !player.hasPermission("thunderchat.bypass.swear")) {
            player.sendMessage(ChatColor.RED + "Please don't use swear words in chat.");
            plugin.getAlertManager().alert("swear", player, message); return true;
        }
        if (containsAdvertisement(message) && !player.hasPermission("thunderchat.bypass.advertisement")) {
            player.sendMessage(ChatColor.RED + "Please don't advertise other Minecraft servers in chat.");
            plugin.getAlertManager().alert("advertisement", player, message); return true;
        }
        if (containsBlockedWord(message) && !player.hasPermission("thunderchat.bypass.filter")) {
            player.sendMessage(ChatColor.RED + "Your message contains a blocked word.");
            plugin.getAlertManager().alert("blocked-words", player, message); return true;
        }
        recordMessage(player.getUniqueId(), message);
        return false;
    }

    public boolean isSpam(Player player, String message) {
        UUID id = player.getUniqueId(); String previous = lastMessage.get(id); Long previousTime = lastMessageTime.get(id);
        if (previous == null || previousTime == null) return false;
        long elapsedSec = Math.max(0L, (System.currentTimeMillis() - previousTime) / 1000L);
        long cooldown = Math.max(1L, plugin.getPluginConfig().getLong("filter.spam.cooldown-seconds", 2L));
        int maxRepeated = Math.max(1, plugin.getPluginConfig().getInt("filter.spam.max-repeated-messages", 3));
        if (elapsedSec > cooldown) { repeatStreak.remove(id); return false; }
        double similarity = ChatSimilarity.similarity(previous, message);
        int streak = repeatStreak.getOrDefault(id, 0);
        streak = similarity >= 0.85 ? streak + 1 : 0; repeatStreak.put(id, streak);
        int score = 0;
        if (similarity >= 0.90) score += 2; else if (similarity >= 0.80) score += 1;
        if (similarity >= 1.0) score += 2;
        if (elapsedSec <= cooldown) score += 2;
        if (streak >= maxRepeated) score += 3;
        int threshold = Math.max(1, plugin.getPluginConfig().getInt("filter.spam.score-threshold", 5));
        return score >= threshold || streak >= maxRepeated;
    }

    public boolean isFlood(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.flood.enabled", true) || message == null || message.isEmpty()) return false;
        int threshold = Math.max(2, plugin.getPluginConfig().getInt("filter.flood.max-consecutive-characters", 3));
        int run = 1; char previous = message.charAt(0);
        for (int i = 1; i < message.length(); i++) {
            char current = message.charAt(i);
            if (current == previous) { if (++run >= threshold) return true; }
            else { previous = current; run = 1; }
        }
        return false;
    }

    public boolean containsSwearWord(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.swear.enabled", true)) return false;
        List<String> configured = plugin.getPluginConfig().getStringList("filter.swear.words");
        if (configured == null || configured.isEmpty()) return false;
        refreshSwearCache(configured);
        String normalized = normalizeForFilter(message);
        for (Pattern pattern : cachedLatinSwears) if (pattern.matcher(normalized).find()) return true;
        for (String candidate : cachedOtherSwears) if (normalized.contains(candidate)) return true;
        return false;
    }

    private void refreshSwearCache(List<String> configured) {
        int hash = configured.hashCode();
        if (hash == swearConfigHash) return;
        synchronized (this) {
            if (hash == swearConfigHash) return;
            java.util.ArrayList<Pattern> latin = new java.util.ArrayList<>();
            java.util.ArrayList<String> other = new java.util.ArrayList<>();
            for (String word : configured) {
                if (word == null || word.isBlank()) continue;
                String candidate = normalizeForFilter(word).trim();
                if (candidate.isEmpty()) continue;
                if (candidate.matches("[a-z0-9_ ]+")) {
                    latin.add(Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(candidate) + "(?![a-z0-9_])"));
                } else other.add(candidate);
            }
            cachedLatinSwears = List.copyOf(latin); cachedOtherSwears = List.copyOf(other); swearConfigHash = hash;
        }
    }

    public boolean containsAdvertisement(String message) {
        if (!plugin.getPluginConfig().getBoolean("filter.advertisement.enabled", true) || message == null || message.isBlank()) return false;
        String normalized = normalizeForFilter(message);
        if (plugin.getPluginConfig().getBoolean("filter.advertisement.block-ip", true) && IPV4_PATTERN.matcher(normalized).find()) return true;
        if (plugin.getPluginConfig().getBoolean("filter.advertisement.block-domains", true) && SERVER_DOMAIN_PATTERN.matcher(normalized).find()) return true;
        for (String name : plugin.getPluginConfig().getStringList("filter.advertisement.server-names")) {
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
        for (String word : blocked) if (word != null && !word.isBlank() && lower.contains(normalizeForFilter(word).trim())) return true;
        return false;
    }

    private String normalizeForFilter(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length()); boolean whitespace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (c == '\u200c' || c == '\u200d' || Character.isWhitespace(c)) {
                if (!whitespace) out.append(' ');
                whitespace = true;
            } else {
                if (c == '\u064a' || c == '\u0649') c = '\u06cc';
                else if (c == '\u0643') c = '\u06a9';
                out.append(c); whitespace = false;
            }
        }
        return out.toString().trim();
    }

    private void recordMessage(UUID id, String message) { lastMessage.put(id, message); lastMessageTime.put(id, System.currentTimeMillis()); }
    public void clear(UUID id) { lastMessage.remove(id); lastMessageTime.remove(id); repeatStreak.remove(id); }
}
