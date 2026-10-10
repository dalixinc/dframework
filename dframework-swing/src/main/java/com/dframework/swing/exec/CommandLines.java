package com.dframework.swing.exec;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Writes commands out the way they'd be typed in a terminal, so people can see and copy what ran. */
public final class CommandLines {

    // ~ and ^ are fine inside a word (HEAD~1) but a shell treats them specially at the start.
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9@%+=:,./_-][A-Za-z0-9@%+=:,./_^~-]*");

    private CommandLines() {
    }

    /**
     * The command as it would be typed in a shell. Arguments that a shell would
     * treat specially are single-quoted, so the text can be pasted and run as is.
     */
    public static String display(List<String> command) {
        return display(Map.of(), command);
    }

    /**
     * As {@link #display(List)}, prefixed with the environment variables the
     * command runs with ({@code NAME=value command ...}), sorted by name.
     */
    public static String display(Map<String, String> environment, List<String> command) {
        StringBuilder line = new StringBuilder();
        new TreeMap<>(environment).forEach((name, value) -> line.append(name).append('=').append(quote(value)).append(' '));
        for (String arg : command) {
            if (line.length() > 0 && line.charAt(line.length() - 1) != ' ') {
                line.append(' ');
            }
            line.append(quote(arg));
        }
        return line.toString().strip();
    }

    /** Quotes one argument for a POSIX shell, if it needs it. */
    public static String quote(String arg) {
        if (!arg.isEmpty() && SAFE.matcher(arg).matches()) {
            return arg;
        }
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}
