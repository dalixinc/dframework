package com.dframework.swing.exec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CommandLinesTest {

    @Test
    void plainArgumentsStayAsTheyAre() {
        assertThat(CommandLines.display(List.of("git", "-C", "/home/dale/repo", "log", "HEAD~1", "main^", "--oneline")))
                .isEqualTo("git -C /home/dale/repo log HEAD~1 main^ --oneline");
    }

    @Test
    void anythingAShellWouldInterpretIsQuoted() {
        assertThat(CommandLines.display(List.of("dig", "a b", "it's", "$(rm)", "*", "")))
                .isEqualTo("dig 'a b' 'it'\\''s' '$(rm)' '*' ''");
    }

    @Test
    void tildeAndCaretAreQuotedAtTheStartOfAWord() {
        // A shell would expand ~/x to the home directory.
        assertThat(CommandLines.quote("~/x")).isEqualTo("'~/x'");
        assertThat(CommandLines.quote("^HEAD")).isEqualTo("'^HEAD'");
        assertThat(CommandLines.quote("HEAD~2")).isEqualTo("HEAD~2");
    }

    @Test
    void environmentVariablesComeFirstSortedByName() {
        assertThat(CommandLines.display(Map.of("GIT_TERMINAL_PROMPT", "0", "GIT_OPTIONAL_LOCKS", "0"), List.of("git", "status")))
                .isEqualTo("GIT_OPTIONAL_LOCKS=0 GIT_TERMINAL_PROMPT=0 git status");
    }
}
