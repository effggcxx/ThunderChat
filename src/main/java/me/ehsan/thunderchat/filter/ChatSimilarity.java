package me.ehsan.thunderchat.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Lightweight message similarity for spam detection.
 * Jaccard (token) similarity for longer messages; LCS for very short ones.
 */
public final class ChatSimilarity {

    private ChatSimilarity() {}

    /**
     * Returns similarity from 0.0 to 1.0.
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }

        a = normalize(a);
        b = normalize(b);

        if (a.equals(b)) {
            return 1.0;
        }

        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        String[] wordsA = a.split(" ");
        String[] wordsB = b.split(" ");

        // Very short messages are better handled as strings.
        if (wordsA.length <= 2 || wordsB.length <= 2) {
            return characterSimilarity(a, b);
        }

        HashSet<String> setA = new HashSet<>(Arrays.asList(wordsA));
        HashSet<String> setB = new HashSet<>(Arrays.asList(wordsB));

        HashSet<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        HashSet<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    private static String normalize(String message) {
        return message
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Cheap character similarity using the longest common subsequence.
     * Only used for very short messages.
     */
    private static double characterSimilarity(String a, String b) {
        int max = Math.max(a.length(), b.length());

        if (max == 0) {
            return 1.0;
        }

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return (double) dp[a.length()][b.length()] / max;
    }
}