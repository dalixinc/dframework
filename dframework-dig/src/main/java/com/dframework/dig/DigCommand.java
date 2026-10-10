package com.dframework.dig;

import java.util.ArrayList;
import java.util.List;

import com.dframework.swing.exec.CommandLines;

/**
 * Turns {@link DigOptions} into the exact argument list that is executed, and
 * into the same command written out for a person to read or paste into a
 * terminal.
 *
 * <p>The arguments are passed straight to the process, with no shell in
 * between, so nothing typed into the form can run another command. Input
 * validation ({@link DigInput}) separately stops a "name" being read by dig as
 * one of its own options.
 */
public final class DigCommand {

    /** The program run. Found on the PATH, as it would be in a terminal. */
    public static final String EXECUTABLE = "dig";

    private DigCommand() {
    }

    /** The full command: program name first, then its arguments. */
    public static List<String> arguments(DigOptions options) {
        List<String> args = new ArrayList<>();
        args.add(EXECUTABLE);
        if (!options.server().isEmpty()) {
            args.add("@" + options.server());
        }
        if (options.has(DigOptions.Flag.REVERSE)) {
            args.add("-x");
            args.add(options.name());
        } else {
            args.add(options.name());
            args.add(options.type());
        }
        for (DigOptions.Flag flag : DigOptions.Flag.values()) {
            if (flag == DigOptions.Flag.REVERSE || !options.has(flag)) {
                continue;
            }
            // +short already prints only the answers, so "answer only" would add nothing.
            if (flag == DigOptions.Flag.ANSWER_ONLY && options.has(DigOptions.Flag.SHORT)) {
                continue;
            }
            args.addAll(flag.arguments());
        }
        if (options.timeoutSeconds() != DigOptions.DEFAULT_TIMEOUT) {
            args.add("+time=" + options.timeoutSeconds());
        }
        return args;
    }

    /**
     * The command as it would be typed in a terminal. Arguments that a shell
     * would treat specially are single-quoted, so the text can be pasted safely.
     */
    public static String display(List<String> arguments) {
        return CommandLines.display(arguments);
    }
}
