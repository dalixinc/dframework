package com.dframework.swing.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.audit.AuditLog;

/** Drives the explanation panel with stand-in assistants; nothing leaves the machine. */
class DExplainPaneTest {

    private final AuditLog audit = AuditLog.inMemory();
    private final Preferences settings = Preferences.userRoot().node("com/dframework/test/" + UUID.randomUUID());

    @AfterEach
    void forgetSettings() throws Exception {
        Preferences parent = settings.parent();
        settings.removeNode();
        if (parent.childrenNames().length == 0) {
            parent.removeNode();
        }
    }

    /** A stand-in assistant: streams the given pieces, or throws, or isn't set up. */
    private record FakeAi(List<String> pieces, AiException failure, String hint) implements AiAssistant {
        public String name() { return "Fake"; }
        public String model() { return "fake-model-1"; }
        public String provider() { return "Nobody"; }
        public Optional<String> setupHint() { return Optional.ofNullable(hint); }
        public AiAnswer ask(AiPrompt prompt, Consumer<String> onText) throws AiException {
            if (failure != null) {
                throw failure;
            }
            pieces.forEach(onText);
            return new AiAnswer(String.join("", pieces), "fake-model-1", 120, 30, "end_turn", 5);
        }
    }

    private static final AiPrompt PROMPT = new AiPrompt("Explain.", "Explain this.");

    private DExplainPane pane(AiAssistant ai) {
        return new DExplainPane(ai, audit, "test.tool", settings);
    }

    private static String answerText(DExplainPane panel) {
        return firstTextArea(panel).getText();
    }

    private static JTextArea firstTextArea(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JTextArea area) {
                return area;
            }
            if (k instanceof Container cc) {
                JTextArea found = firstTextArea(cc);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void waitUntilIdle(DExplainPane panel) throws Exception {
        for (int i = 0; i < 100; i++) {
            boolean[] running = new boolean[1];
            SwingUtilities.invokeAndWait(() -> running[0] = panel.isRunning());
            if (!running[0]) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("explanation never finished");
    }

    @Test
    void showsTheStreamedExplanationAndAuditsIt() throws Exception {
        DExplainPane[] panel = new DExplainPane[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = pane(new FakeAi(List.of("gmail.com uses ", "five mail servers."), null, null));
            panel[0].explain("dig gmail.com MX", PROMPT);
        });
        waitUntilIdle(panel[0]);

        assertThat(answerText(panel[0])).isEqualTo("gmail.com uses five mail servers.");
        assertThat(audit.recent()).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("explain");
            Map<String, String> d = e.details();
            assertThat(d).containsEntry("command", "dig gmail.com MX").containsEntry("outcome", "ok")
                    .containsEntry("model", "fake-model-1").containsEntry("inputTokens", "120").containsEntry("outputTokens", "30");
        });
    }

    @Test
    void whenNotSetUpItShowsHowAndSendsNothing() throws Exception {
        DExplainPane[] panel = new DExplainPane[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = pane(new FakeAi(List.of(), null, "Set FAKE_KEY first."));
            panel[0].explain("dig example.com A", PROMPT);
        });

        assertThat(panel[0].isRunning()).isFalse();
        assertThat(answerText(panel[0])).isEqualTo("Set FAKE_KEY first.");
        assertThat(audit.recent()).as("nothing was sent, so nothing to audit").isEmpty();
    }

    @Test
    void failuresAreShownAndAudited() throws Exception {
        AiException rateLimited = new AiException(AiException.Kind.RATE_LIMITED, "Busy, try again shortly.", null);
        DExplainPane[] panel = new DExplainPane[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = pane(new FakeAi(List.of(), rateLimited, null));
            panel[0].explain("dig example.com A", PROMPT);
        });
        waitUntilIdle(panel[0]);

        assertThat(answerText(panel[0])).contains("Busy, try again shortly.");
        assertThat(audit.recent()).singleElement().satisfies(e ->
                assertThat(e.details()).containsEntry("outcome", "rate_limited"));
    }
}
