package com.dframework.git;

import java.util.Optional;
import java.util.regex.Pattern;

import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

/**
 * What may be typed into the browser. Nothing can start with {@code -}, so it
 * can never be read as a git option; for example, {@code --output=<file>}
 * would make some commands write a file. The commands also put such values
 * after {@code --end-of-options} or {@code --}, so this is a second line of
 * defence, not the only one.
 */
public final class GitInput {

    /** Revision syntax: names, ids, HEAD~2, main^, @{1}, HEAD^{tree}, v1.0..v2.0. */
    private static final Pattern REVISION = Pattern.compile("[A-Za-z0-9._/@^~{}+-]+");

    private GitInput() {
    }

    /** While typing a revision: no spaces, no leading '-'. */
    public static InputFilter revisionFilter() {
        return InputFilter.regex("(?!-)[A-Za-z0-9._/@^~{}+-]*").and(InputFilter.maxLength(200));
    }

    /** While typing an object: a revision, optionally followed by ':' and a path (which may contain spaces). */
    public static InputFilter objectFilter() {
        return InputFilter.regex("(?!-)[^\\x00-\\x1f]*").and(InputFilter.maxLength(400));
    }

    /** While typing a path: anything printable, but not a leading '-'. */
    public static InputFilter pathFilter() {
        return InputFilter.regex("(?!-)[^\\x00-\\x1f]*").and(InputFilter.maxLength(400));
    }

    /** A revision such as HEAD, main, v1.2, a1b2c3d or HEAD~2. */
    public static Validator revisionValidator() {
        return Validator.required().and(text -> {
            String value = text.strip();
            if (value.startsWith("-")) {
                return Optional.of("A revision can't start with -");
            }
            return REVISION.matcher(value).matches() ? Optional.empty()
                    : Optional.of("Enter a revision, such as HEAD, main, v1.2 or a commit id");
        });
    }

    /**
     * An object: a revision, or revision:path (such as {@code HEAD:README.md}),
     * or an object id.
     */
    public static Validator objectValidator() {
        return Validator.required().and(text -> {
            String value = text.strip();
            if (value.startsWith("-")) {
                return Optional.of("An object can't start with -");
            }
            int colon = value.indexOf(':');
            String revision = colon < 0 ? value : value.substring(0, colon);
            if (!revision.isEmpty() && !REVISION.matcher(revision).matches()) {
                return Optional.of("Enter an object, such as HEAD, HEAD:README.md or an object id");
            }
            return value.chars().anyMatch(c -> c < 0x20) ? Optional.of("Control characters aren't allowed")
                    : Optional.empty();
        });
    }

    /** A file path within the repository. */
    public static Validator pathValidator() {
        return Validator.required().and(text -> {
            String value = text.strip();
            if (value.startsWith("-")) {
                return Optional.of("A path can't start with -; use ./ in front if the file name does");
            }
            return value.chars().anyMatch(c -> c < 0x20) ? Optional.of("Control characters aren't allowed")
                    : Optional.empty();
        });
    }

    /** Throws if {@code validator} rejects {@code value}. */
    static void check(Validator validator, String value) {
        validator.validate(value).ifPresent(message -> {
            throw new IllegalArgumentException(message);
        });
    }
}
