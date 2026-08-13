package me.ehsan.thunderchat.filter;

import me.ehsan.thunderchat.ThunderChat;

public class FilterManager {

    private final ThunderChat plugin;

    public FilterManager(ThunderChat plugin) {
        this.plugin = plugin;
        // TODO: load spam/caps/word-filter thresholds from config.yml
    }

    // TODO: isSpam(player, message), isExcessiveCaps(message), containsBlockedWord(message)
    // TODO: bypass check via thunderchat.bypass.filter permission
}
