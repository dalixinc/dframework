package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

class DigInputTest {

    private final Validator forward = DigInput.nameValidator(() -> false);
    private final Validator reverse = DigInput.nameValidator(() -> true);

    @Test
    void acceptsRealNames() {
        for (String ok : new String[] {"example.com", "example.com.", "_dmarc.example.com", "*.example.com", "localhost", ".", "xn--bcher-kva.example"}) {
            assertThat(forward.validate(ok)).as(ok).isEmpty();
        }
    }

    @Test
    void rejectsMalformedNames() {
        for (String bad : new String[] {"", "-example.com", "exa mple.com", "example..com", "ex-.com", "a".repeat(64) + ".com"}) {
            assertThat(forward.validate(bad)).as(bad).isPresent();
        }
    }

    @Test
    void reverseLookupsNeedAnIpAddress() {
        assertThat(reverse.validate("8.8.8.8")).isEmpty();
        assertThat(reverse.validate("2001:4860:4860::8888")).isEmpty();
        assertThat(reverse.validate("example.com")).isPresent();
        assertThat(reverse.validate("999.1.1.1")).isPresent();
    }

    @Test
    void textThatDigWouldReadAsAnOptionIsBlocked() {
        // "-f /etc/passwd" would make dig read queries from a local file.
        InputFilter filter = DigInput.nameFilter();
        for (String attempt : new String[] {"-f", "+trace", "@evil", "-f /etc/passwd", "a b", "x;rm", "$(id)"}) {
            assertThat(filter.accept(attempt)).as("filter: " + attempt).isFalse();
        }
        for (String attempt : new String[] {"-f", "+trace", "@evil"}) {
            assertThat(forward.validate(attempt)).as("validator: " + attempt).isPresent();
            assertThat(DigInput.serverValidator().validate(attempt)).as("server: " + attempt).isPresent();
        }
        assertThat(filter.accept("mail-01.example.com")).isTrue();
    }

    @Test
    void serverMayBeBlankAnAddressOrAHost() {
        Validator server = DigInput.serverValidator();
        assertThat(server.validate("")).isEmpty();
        assertThat(server.validate("1.1.1.1")).isEmpty();
        assertThat(server.validate("dns.google")).isEmpty();
        assertThat(server.validate("not a server")).isPresent();
    }
}
