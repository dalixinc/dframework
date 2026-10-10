package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dframework.dig.DigOptions.Flag;

class DigCommandTest {

    private static DigOptions options(String name, String type, String server, Set<Flag> flags, int timeout) {
        return new DigOptions(name, type, server, flags, timeout);
    }

    @Test
    void plainLookup() {
        List<String> args = DigCommand.arguments(options("example.com", "A", "", Set.of(), 5));

        assertThat(args).containsExactly("dig", "example.com", "A");
        assertThat(DigCommand.display(args)).isEqualTo("dig example.com A");
    }

    @Test
    void serverFlagsAndTimeoutInAStableOrder() {
        List<String> args = DigCommand.arguments(options("gmail.com", "MX", "1.1.1.1",
                EnumSet.of(Flag.TCP, Flag.TRACE, Flag.DNSSEC), 10));

        assertThat(DigCommand.display(args)).isEqualTo("dig @1.1.1.1 gmail.com MX +trace +dnssec +tcp +time=10");
    }

    @Test
    void reverseLookupUsesDashXAndDropsTheType() {
        List<String> args = DigCommand.arguments(options("8.8.8.8", "MX", "", EnumSet.of(Flag.REVERSE), 5));

        assertThat(args).containsExactly("dig", "-x", "8.8.8.8");
    }

    @Test
    void answerOnlyIsDroppedWhenShortIsOn() {
        List<String> args = DigCommand.arguments(options("example.com", "A", "",
                EnumSet.of(Flag.SHORT, Flag.ANSWER_ONLY), 5));

        assertThat(args).containsExactly("dig", "example.com", "A", "+short");
    }

    @Test
    void answerOnlyAddsBothArguments() {
        assertThat(DigCommand.arguments(options("example.com", "A", "", EnumSet.of(Flag.ANSWER_ONLY), 5)))
                .containsExactly("dig", "example.com", "A", "+noall", "+answer");
    }

    @Test
    void displayQuotesAnythingAShellWouldInterpret() {
        assertThat(DigCommand.display(List.of("dig", "a b", "it's", "$(rm)", "*")))
                .isEqualTo("dig 'a b' 'it'\\''s' '$(rm)' '*'");
    }
}
