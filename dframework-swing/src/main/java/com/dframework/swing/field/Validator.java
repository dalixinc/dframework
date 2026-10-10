package com.dframework.swing.field;

import java.math.BigInteger;
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

    /**
     * A practical email address check: dot-separated local part, then a
     * domain of hyphen-safe labels ending in a top-level domain of two or more
     * letters. Rarely used forms such as quoted local parts are rejected.
     */
    static Validator email() {
        String atom = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
        String label = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
        return matches(atom + "(?:\\." + atom + ")*@(?:" + label + "\\.)+[A-Za-z]{2,}",
                DResources.message("validation.email"));
    }

    /** A whole number: ASCII digits with an optional leading minus sign. */
    static Validator integer() {
        // ASCII only: Long.parseLong would also accept "+5" and Arabic-Indic digits.
        Pattern integer = Pattern.compile("-?[0-9]+");
        return optional(of(text -> integer.matcher(text.strip()).matches(), DResources.message("validation.integer")));
    }

    /** A whole number from {@code min} to {@code max} inclusive. */
    static Validator integer(long min, long max) {
        String message = MessageFormat.format(DResources.message("validation.range"),
                String.valueOf(min), String.valueOf(max));
        BigInteger low = BigInteger.valueOf(min);
        BigInteger high = BigInteger.valueOf(max);
        // BigInteger, so a 30-digit entry gets the range message rather than overflowing.
        return integer().and(optional(of(text -> {
            BigInteger value = new BigInteger(text.strip());
            return value.compareTo(low) >= 0 && value.compareTo(high) <= 0;
        }, message)));
    }

    /** Lets empty text through, so only fields marked {@link #required()} insist on a value. */
    private static Validator optional(Validator validator) {
        return text -> text.isEmpty() ? Optional.empty() : validator.validate(text);
    }
}
