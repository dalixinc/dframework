package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.ai.AiAnswer;
import com.dframework.swing.ai.AiAssistant;
import com.dframework.swing.ai.AiException;
import com.dframework.swing.ai.AiPrompt;
import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.command.Command;
import com.dframework.swing.plugin.DPlugin;
import com.dframework.swing.plugin.PluginContext;

/** Drives the explanation tab with stand-in assistants; nothing leaves the machine. */
class DigExplainPanelTest {

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

    private PluginContext context() {
        DPlugin plugin = new DigPlugin();
        return new PluginContext() {
            public DPlugin plugin() { return plugin; }
            public JComponent component() { return null; }
            public AuditLog auditLog() { return audit; }
            public Preferences preferences() { return settings; }
            public void addCommand(Command command) { }
        };
    }

    private static String answerText(DigExplainPanel panel) {
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

    private static void waitUntilIdle(DigExplainPanel panel) throws Exception {
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
        DigExplainPanel[] panel = new DigExplainPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DigExplainPanel(context(), new FakeAi(List.of("gmail.com uses ", "five mail servers."), null, null));
            panel[0].explain("dig gmail.com MX", 0, List.of("gmail.com. 300 IN MX 5 a.example."));
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
        DigExplainPanel[] panel = new DigExplainPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DigExplainPanel(context(), new FakeAi(List.of(), null, "Set FAKE_KEY first."));
            panel[0].explain("dig example.com A", 0, List.of("output"));
        });

        assertThat(panel[0].isRunning()).isFalse();
        assertThat(answerText(panel[0])).isEqualTo("Set FAKE_KEY first.");
        assertThat(audit.recent()).as("nothing was sent, so nothing to audit").isEmpty();
    }

    @Test
    void failuresAreShownAndAudited() throws Exception {
        AiException rateLimited = new AiException(AiException.Kind.RATE_LIMITED, "Busy, try again shortly.", null);
        DigExplainPanel[] panel = new DigExplainPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DigExplainPanel(context(), new FakeAi(List.of(), rateLimited, null));
            panel[0].explain("dig example.com A", 0, List.of("output"));
        });
        waitUntilIdle(panel[0]);

        assertThat(answerText(panel[0])).contains("Busy, try again shortly.");
        assertThat(audit.recent()).singleElement().satisfies(e ->
                assertThat(e.details()).containsEntry("outcome", "rate_limited"));
    }
}
