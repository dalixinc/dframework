package com.dframework.swing.field;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.dframework.swing.util.DResources;

/**
 * Checks the text of a {@link DTextField}.
 *
 * <p>Apart from {@link #required()}, the built-in validators accept empty text,
 * so an optional field is only checked once something is typed. Combine them
 * with {@link #and(Validator)}:
 *
 * <pre>{@code
 * field.setValidator(Validator.required().and(Validator.email()));
 * }</pre>
 */
@FunctionalInterface
public interface Validator {

    /**
     * @param text the field's current text, never {@code null}
     * @return an error message for the user, or empty if the text is acceptable
     */
    Optional<String> validate(String text);

    /** Runs this validator, then {@code next} if this one passed. */
    default Validator and(Validator next) {
        Objects.requireNonNull(next, "next");
        return text -> {
            Optional<String> error = validate(text);
            return error.isPresent() ? error : next.validate(text);
        };
    }

    /** A validator that fails with {@code message} when {@code test} is false. */
    static Validator of(Predicate<String> test, String message) {
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(message, "message");
        return text -> test.test(text) ? Optional.empty() : Optional.of(message);
    }

    /** Text must not be blank. */
    static Validator required() {
        return of(text -> !text.isBlank(), DResources.message("validation.required"));
    }

    /** At most {@code max} characters. */
    static Validator maxLength(int max) {
        return of(text -> text.length() <= max,
                MessageFormat.format(DResources.message("validation.maxLength"), String.valueOf(max)));
    }

    /** The whole text must match {@code regex}. */
    static Validator matches(String regex, String message) {
        Pattern pattern = Pattern.compile(regex);
        return optional(of(text -> pattern.matcher(text).matches(), message));
    }

    /** A plausible email address (something@something.something). */
    static Validator email() {
        return matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+", DResources.message("validation.email"));
    }

    /** A whole number. */
    static Validator integer() {
        return optional(of(Validator::isInteger, DResources.message("validation.integer")));
    }

    /** A whole number from {@code min} to {@code max} inclusive. */
    static Validator integer(long min, long max) {
        String message = MessageFormat.format(DResources.message("validation.range"),
                String.valueOf(min), String.valueOf(max));
        return integer().and(optional(of(text -> {
            long value = Long.parseLong(text.strip());
            return value >= min && value <= max;
        }, message)));
    }

    private static boolean isInteger(String text) {
        try {
            Long.parseLong(text.strip());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Lets empty text through, so only fields marked {@link #required()} insist on a value. */
    private static Validator optional(Validator validator) {
        return text -> text.isEmpty() ? Optional.empty() : validator.validate(text);
    }
}
