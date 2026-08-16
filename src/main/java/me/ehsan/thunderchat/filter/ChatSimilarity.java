package me.ehsan.thunderchat.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/** Lightweight message similarity for spam detection. */
public final class ChatSimilarity {
    private ChatSimilarity() {}

    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        a = normalize(a); b = normalize(b);
        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        String[] wordsA = a.split(" "); String[] wordsB = b.split(" ");
        if (wordsA.length <= 2 || wordsB.length <= 2) return characterSimilarity(a, b);
        HashSet<String> setA = new HashSet<>(Arrays.asList(wordsA)); HashSet<String> setB = new HashSet<>(Arrays.asList(wordsB));
        HashSet<String> intersection = new HashSet<>(setA); intersection.retainAll(setB);
        HashSet<String> union = new HashSet<>(setA); union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static String normalize(String message) {
        return message.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
    }

    /** O(n*m) time but only O(min(n,m)) memory instead of a full matrix. */
    private static double characterSimilarity(String a, String b) {
        if (a.length() < b.length()) { String tmp = a; a = b; b = tmp; }
        int max = a.length(); if (max == 0) return 1.0;
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                current[j] = a.charAt(i - 1) == b.charAt(j - 1) ? previous[j - 1] + 1 : Math.max(previous[j], current[j - 1]);
            }
            int[] swap = previous; previous = current; current = swap;
            Arrays.fill(current, 0);
        }
        return (double) previous[b.length()] / max;
    }
}
