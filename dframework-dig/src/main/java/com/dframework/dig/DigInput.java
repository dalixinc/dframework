package com.dframework.dig;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

/**
 * What may be typed into the dig form, and what counts as valid.
 *
 * <p>The important rule is that a name or server can never begin with
 * {@code -}, {@code +} or {@code @}. Those would be read by dig as its own
 * options: for example {@code -f /etc/passwd} makes dig read queries from a
 * local file. The filters block such text as it is typed, and the validators
 * reject it if it arrives any other way.
 */
public final class DigInput {

    private static final Pattern LABEL = Pattern.compile("(?:\\*|[A-Za-z0-9_](?:[A-Za-z0-9_-]{0,61}[A-Za-z0-9_])?)");
    private static final Pattern HOSTNAME = Pattern.compile("(?:" + LABEL + "\\.)*" + LABEL + "\\.?|\\.");
    private static final Pattern IPV4 = Pattern.compile(
            "(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:.]*:[0-9A-Fa-f:.]*");

    private DigInput() {
    }

    /** Characters allowed while typing a name: letters, digits, dot, hyphen, underscore, colon, asterisk. */
    public static InputFilter nameFilter() {
        return InputFilter.regex("(?![-+@])[A-Za-z0-9._:*-]*").and(InputFilter.maxLength(253));
    }

    /** Characters allowed while typing a server. */
    public static InputFilter serverFilter() {
        return InputFilter.regex("(?![-+@])[A-Za-z0-9._:-]*").and(InputFilter.maxLength(253));
    }

    /** A domain name, or an IP address when {@code reverse} says so. */
    public static Validator nameValidator(BooleanSupplier reverse) {
        return Validator.required().and(text -> {
            String value = text.strip();
            if (startsLikeAnOption(value)) {
                return Optional.of("A name can't start with -, + or @");
            }
            if (value.length() > 253) {
                return Optional.of("Names are at most 253 characters");
            }
            if (reverse.getAsBoolean()) {
                return isIpAddress(value) ? Optional.empty()
                        : Optional.of("A reverse lookup needs an IP address, like 8.8.8.8");
            }
            return HOSTNAME.matcher(value).matches() ? Optional.empty()
                    : Optional.of("Enter a domain name, like example.com");
        });
    }

    /** A host name or IP address, or blank for the system resolver. */
    public static Validator serverValidator() {
        return text -> {
            String value = text.strip();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            if (startsLikeAnOption(value)) {
                return Optional.of("A server can't start with -, + or @");
            }
            return isIpAddress(value) || HOSTNAME.matcher(value).matches() ? Optional.empty()
                    : Optional.of("Enter an IP address or host name, or leave blank for the system resolver");
        };
    }

    static boolean isIpAddress(String value) {
        return IPV4.matcher(value).matches() || IPV6.matcher(value).matches() && value.chars().filter(c -> c == ':').count() >= 2;
    }

    private static boolean startsLikeAnOption(String value) {
        return !value.isEmpty() && "-+@".indexOf(value.charAt(0)) >= 0;
    }
}
