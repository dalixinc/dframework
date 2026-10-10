package com.dframework.swing.command;

import java.util.Locale;

/**
 * Scores how well a query matches some text, in the style of editor command
 * palettes: the query's characters must appear in order, and matches score
 * higher when they start words or run consecutively. So "fdark" finds
 * "FlatLaf Dark", and "tfd" ranks "Theme FlatLaf Dark" well.
 *
 * <p>Uses dynamic programming over (query character, text position), so the
 * best alignment wins, not just the first one found left to right.
 */
final class FuzzyMatcher {

    /** A successful match: higher scores are better; positions index into the text. */
    record Match(double score, int[] positions) {
    }

    private static final double MATCH = 1;
    private static final double WORD_START = 8;
    private static final double TEXT_START = 4;
    private static final double CONSECUTIVE = 5;
    private static final double GAP_START = 3;
    private static final double GAP_EXTENSION = 1;
    private static final double LEADING_GAP = 0.05;
    private static final double NONE = Double.NEGATIVE_INFINITY;

    private FuzzyMatcher() {
    }

    /**
     * @return the best match, or {@code null} if the query's characters do not
     *         all appear in order. Spaces in the query are ignored, and an
     *         empty query matches everything with score 0.
     */
    static Match match(String query, String text) {
        String q = query.replace(" ", "").toLowerCase(Locale.ROOT);
        String t = text.toLowerCase(Locale.ROOT);
        int m = q.length();
        int n = t.length();
        if (m == 0) {
            return new Match(0, new int[0]);
        }
        if (m > n) {
            return null;
        }
        // score[i][j]: best score with q[i] matched at t[j].
        // from[i][j]: where q[i-1] matched on that best path.
        // run[i][j]: bonus of the first character of the consecutive run ending at j. As in fzf,
        // letters continuing a run inherit it, so a whole word beats scattered word-initial letters.
        double[][] score = new double[m][n];
        int[][] from = new int[m][n];
        double[][] run = new double[m][n];
        for (int i = 0; i < m; i++) {
            // Gap of g >= 1 skipped characters costs GAP_START + GAP_EXTENSION * (g - 1). For a
            // predecessor at k <= j - 2 that is linear in k, so a running maximum of
            // score[i-1][k] + GAP_EXTENSION * k finds the best gapped predecessor in O(1).
            double bestGapped = NONE;
            int bestGappedAt = -1;
            for (int j = 0; j < n; j++) {
                if (i > 0 && j >= 2 && score[i - 1][j - 2] != NONE) {
                    double candidate = score[i - 1][j - 2] + GAP_EXTENSION * (j - 2);
                    if (candidate > bestGapped) {
                        bestGapped = candidate;
                        bestGappedAt = j - 2;
                    }
                }
                score[i][j] = NONE;
                if (q.charAt(i) != t.charAt(j)) {
                    continue;
                }
                double boundary = (isWordStart(text, j) ? WORD_START : 0) + (j == 0 ? TEXT_START : 0);
                if (i == 0) {
                    score[i][j] = MATCH + boundary - LEADING_GAP * j;
                    from[i][j] = -1;
                    run[i][j] = boundary;
                    continue;
                }
                double viaGap = bestGapped == NONE ? NONE
                        : bestGapped - GAP_EXTENSION * j + 2 * GAP_EXTENSION - GAP_START + MATCH + boundary;
                double viaConsecutive = NONE;
                double runBonus = boundary;
                if (j >= 1 && score[i - 1][j - 1] != NONE) {
                    runBonus = Math.max(boundary, run[i - 1][j - 1]);
                    viaConsecutive = score[i - 1][j - 1] + MATCH + Math.max(runBonus, CONSECUTIVE);
                }
                if (viaConsecutive != NONE && viaConsecutive >= viaGap) {
                    score[i][j] = viaConsecutive;
                    from[i][j] = j - 1;
                    run[i][j] = runBonus;
                } else if (viaGap != NONE) {
                    score[i][j] = viaGap;
                    from[i][j] = bestGappedAt;
                    run[i][j] = boundary;
                }
            }
        }
        int end = -1;
        double best = NONE;
        for (int j = 0; j < n; j++) {
            if (score[m - 1][j] > best) {
                best = score[m - 1][j];
                end = j;
            }
        }
        if (end < 0) {
            return null;
        }
        int[] positions = new int[m];
        for (int i = m - 1, j = end; i >= 0; i--) {
            positions[i] = j;
            j = from[i][j];
        }
        return new Match(best, positions);
    }

    /** Start of the text, after a separator, or an upper-case letter after a lower-case one (camelCase). */
    private static boolean isWordStart(String text, int index) {
        if (index == 0) {
            return true;
        }
        char prev = text.charAt(index - 1);
        char c = text.charAt(index);
        return !Character.isLetterOrDigit(prev) || Character.isLowerCase(prev) && Character.isUpperCase(c);
    }
}
