package com.dframework.swing.field;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Decides what a {@link DTextField} lets the user type. Edits (keystrokes,
 * pastes, drops) that would produce rejected text are blocked with the look
 * and feel's error beep.
 *
 * <p>Filters stop bad input getting in; {@link Validator}s judge whether what
 * is in there is acceptable. Use both: a filter of {@code integer()} stops
 * letters in an age field, while the validator {@code Validator.integer(0, 130)}
 * still flags 200 (a filter can't, because "2" is on the way to "20").
 *
 * <pre>{@code
 * age.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));
 * }</pre>
 */
@FunctionalInterface
public interface InputFilter {

    /**
     * @param proposedText the whole text the field would contain after the edit
     * @return whether to allow the edit
     */
    boolean accept(String proposedText);

    /** Allows an edit only if both filters do. */
    default InputFilter and(InputFilter other) {
        Objects.requireNonNull(other, "other");
        return text -> accept(text) && other.accept(text);
    }

    /** At most {@code max} characters. */
    static InputFilter maxLength(int max) {
        return text -> text.length() <= max;
    }

    /** ASCII digits 0-9 only. */
    static InputFilter digits() {
        return regex("[0-9]*");
    }

    /** ASCII digits with an optional leading minus sign. */
    static InputFilter integer() {
        return regex("-?[0-9]*");
    }

    /** Only characters from {@code allowed}, e.g. {@code chars("ABCDEFabcdef0123456789")}. */
    static InputFilter chars(String allowed) {
        return text -> text.chars().allMatch(c -> allowed.indexOf(c) >= 0);
    }

    /**
     * The whole text must match {@code regex}. It must also match every
     * partly-typed state on the way, so write it to allow those: for example
     * {@code "[A-Z]{0,3}"} rather than {@code "[A-Z]{3}"}.
     */
    static InputFilter regex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return text -> pattern.matcher(text).matches();
    }
}
