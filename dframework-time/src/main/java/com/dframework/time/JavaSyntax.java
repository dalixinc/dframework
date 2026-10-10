package com.dframework.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Splits Java source into coloured spans: keywords, types, strings, numbers
 * and comments. Simple and forgiving rather than complete: it only has to
 * make short examples readable on a projector.
 */
final class JavaSyntax {

    enum Kind { KEYWORD, TYPE, STRING, NUMBER, COMMENT }

    record Span(int start, int length, Kind kind) {
    }

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do",
            "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "new", "null", "package", "private", "protected",
            "public", "record", "return", "short", "static", "super", "switch", "this", "throw", "throws", "true",
            "try", "var", "void", "while", "yield");

    private JavaSyntax() {
    }

    static List<Span> spans(String text) {
        List<Span> spans = new ArrayList<>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                int end = text.indexOf('\n', i);
                end = end < 0 ? n : end;
                spans.add(new Span(i, end - i, Kind.COMMENT));
                i = end;
            } else if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? n : end + 2;
                spans.add(new Span(i, end - i, Kind.COMMENT));
                i = end;
            } else if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < n && text.charAt(end) != c && text.charAt(end) != '\n') {
                    end += text.charAt(end) == '\\' ? 2 : 1;
                }
                end = Math.min(n, end + 1);
                spans.add(new Span(i, end - i, Kind.STRING));
                i = end;
            } else if (Character.isDigit(c)) {
                int end = i + 1;
                while (end < n && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_'
                        || text.charAt(end) == '.' && end + 1 < n && Character.isDigit(text.charAt(end + 1)))) {
                    end++;
                }
                spans.add(new Span(i, end - i, Kind.NUMBER));
                i = end;
            } else if (Character.isJavaIdentifierStart(c)) {
                int end = i + 1;
                while (end < n && Character.isJavaIdentifierPart(text.charAt(end))) {
                    end++;
                }
                String word = text.substring(i, end);
                if (KEYWORDS.contains(word)) {
                    spans.add(new Span(i, end - i, Kind.KEYWORD));
                } else if (Character.isUpperCase(c) && !isConstant(word)) {
                    spans.add(new Span(i, end - i, Kind.TYPE));
                }
                i = end;
            } else {
                i++;
            }
        }
        return spans;
    }

    /** FEBRUARY, MAX_VALUE: constants, left plain rather than coloured as types. */
    private static boolean isConstant(String word) {
        return word.length() > 1 && word.chars().allMatch(ch -> Character.isUpperCase(ch) || Character.isDigit(ch) || ch == '_');
    }
}
