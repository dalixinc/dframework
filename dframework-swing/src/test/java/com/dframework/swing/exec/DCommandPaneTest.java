package com.dframework.swing.exec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.dframework.swing.audit.AuditLog;

class DCommandPaneTest {

    private final AuditLog log = AuditLog.inMemory();

    @Test
    void showsTheCommandThatWillRun() {
        DCommandPane pane = new DCommandPane(log, "tool", "run");

        pane.setCommand("git status --short");

        assertThat(pane.getCommand()).isEqualTo("git status --short");
    }

    @Test
    void historyFollowsTheAuditLogForItsSourceAndActions() throws Exception {
        log.record("tool", "run", Map.of("command", "earlier"));
        DCommandPane pane = new DCommandPane(log, "tool", "run", "inspect");
        assertThat(pane.getHistorySize()).isEqualTo(1);

        log.record("tool", "inspect", Map.of("command", "git cat-file -t HEAD"));
        log.record("tool", "explain", Map.of("command", "not a command run"));
        log.record("other", "run", Map.of("command", "someone else's"));
        SwingUtilities.invokeAndWait(() -> { });

        assertThat(pane.getHistorySize()).isEqualTo(2);
        pane.dispose();
        log.record("tool", "run", Map.of("command", "after dispose"));
        SwingUtilities.invokeAndWait(() -> { });
        assertThat(pane.getHistorySize()).isEqualTo(2);
    }
}
