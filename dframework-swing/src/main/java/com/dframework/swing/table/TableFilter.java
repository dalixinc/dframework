package com.dframework.swing.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and applies {@link DTable}'s filter syntax:
 *
 * <ul>
 * <li>{@code acme printer}: every word must appear somewhere in the row
 * <li>{@code "printer fault"}: a quoted phrase must appear as written
 * <li>{@code -closed}: rows containing the word are excluded
 * <li>{@code status:open}: the word must appear in a column whose name starts
 * with "status"; quotes and minus work here too ({@code -status:closed})
 * </ul>
 *
 * Matching ignores case. If {@code xyz:} names no column, the whole token
 * (colon included) is searched for as ordinary text, so URLs and times work.
 */
final class TableFilter {

    /**
     * One condition. {@code column} is a model index, or -1 for any column;
     * {@code text} is lower-case.
     */
    record Term(int column, String text, boolean negated) {
    }

    private static final Pattern TOKEN = Pattern.compile("(-)?(?:([^\\s:\"]+):)?(?:\"([^\"]*)\"?|(\\S+))");

    private TableFilter() {
    }

    /**
     * @param query        what the user typed
     * @param columnNames  the model's column names, by model index
     */
    static List<Term> parse(String query, List<String> columnNames) {
        List<Term> terms = new ArrayList<>();
        Matcher m = TOKEN.matcher(query);
        while (m.find()) {
            boolean negated = m.group(1) != null;
            String prefix = m.group(2);
            String text = m.group(3) != null ? m.group(3) : m.group(4);
            if (text == null) {
                continue;
            }
            int column = -1;
            if (prefix != null) {
                column = resolveColumn(prefix, columnNames);
                if (column < 0) {
                    text = prefix + ":" + text;   // not a column, e.g. "10:30" or "http://..."
                }
            }
            if (!text.isEmpty()) {
                terms.add(new Term(column, text.toLowerCase(Locale.ROOT), negated));
            }
        }
        return terms;
    }

    /** Exact (case-insensitive) column name first, then the first name starting with the prefix. */
    private static int resolveColumn(String prefix, List<String> columnNames) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).toLowerCase(Locale.ROOT).equals(p)) {
                return i;
            }
        }
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).toLowerCase(Locale.ROOT).startsWith(p)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param cell         the display text of a cell, by model column index
     * @param columnCount  number of model columns
     */
    static boolean matches(List<Term> terms, IntFunction<String> cell, int columnCount) {
        for (Term term : terms) {
            boolean found = false;
            if (term.column() >= 0) {
                found = contains(cell.apply(term.column()), term.text());
            } else {
                for (int c = 0; c < columnCount && !found; c++) {
                    found = contains(cell.apply(c), term.text());
                }
            }
            if (found == term.negated()) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(String cell, String lowerText) {
        return cell != null && cell.toLowerCase(Locale.ROOT).contains(lowerText);
    }
}
