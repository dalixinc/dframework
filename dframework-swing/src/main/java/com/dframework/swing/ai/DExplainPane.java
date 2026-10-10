package com.dframework.swing.ai;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.Serial;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import java.util.prefs.Preferences;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.panel.DCollapsiblePane;

/**
 * An "AI explanation" panel: an assistant's explanation of some result,
 * written in as it arrives, with exactly what was sent available beneath.
 *
 * <ul>
 * <li><b>Disclosure:</b> it says where the data goes.
 * <li><b>Footer:</b> shows the model, tokens and time.
 * <li><b>Audit:</b> each explanation is recorded as an {@code "explain"} entry.
 * <li><b>Not set up:</b> it shows the assistant's setup hint and sends nothing.
 * </ul>
 *
 * <pre>{@code
 * DExplainPane explain = new DExplainPane(ai, auditLog, "com.example.tool", prefs);
 * explain.explain("ping -c 3 example.com", new AiPrompt(system, user));
 * }</pre>
 */
public class DExplainPane extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient AiAssistant ai;
    private final transient AuditLog log;
    private final String source;
    private final JTextArea answer = new JTextArea();
    private final JTextArea sent = new JTextArea();
    private final JLabel footer = new JLabel(" ");
    private final DCollapsiblePane sentPane;
    private transient SwingWorker<AiAnswer, String> worker;
    private transient Runnable onStateChange = () -> { };

    /**
     * @param source      the audit source for "explain" entries, e.g. a plugin id
     * @param preferences where to remember whether "What was sent" is open
     */
    public DExplainPane(AiAssistant ai, AuditLog log, String source, Preferences preferences) {
        super(new BorderLayout(0, 6));
        this.ai = ai;
        this.log = log;
        this.source = source;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel disclosure = new JLabel(ai.name() + " (" + ai.model() + "). Explaining sends the command and its output to "
                + ai.provider() + ".");
        disclosure.setEnabled(false);
        add(disclosure, BorderLayout.NORTH);

        answer.setEditable(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        answer.setFont(answer.getFont().deriveFont(answer.getFont().getSize2D() + 1f));
        answer.setText(ai.setupHint().orElse("Run a command, then press \"Explain with AI\" to have "
                + ai.name() + " explain the result in plain English."));
        answer.getAccessibleContext().setAccessibleName("AI explanation");
        add(new JScrollPane(answer), BorderLayout.CENTER);

        sent.setEditable(false);
        sent.setLineWrap(true);
        sent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, sent.getFont().getSize()));
        sent.setRows(8);
        sentPane = new DCollapsiblePane("What was sent to " + ai.name(), new JScrollPane(sent), false);
        sentPane.rememberState(preferences, "explain.sent.expanded");

        JPanel south = new JPanel(new BorderLayout(0, 4));
        footer.setEnabled(false);
        south.add(footer, BorderLayout.NORTH);
        south.add(sentPane, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    /** Called whenever an explanation starts or finishes, so buttons can update. */
    public void setOnStateChange(Runnable onStateChange) {
        this.onStateChange = onStateChange;
    }

    public boolean isRunning() {
        return worker != null;
    }

    /** The assistant used. */
    public AiAssistant getAssistant() {
        return ai;
    }

    /** Shows a message in place of an explanation, e.g. why there's nothing to explain. */
    public void showMessage(String message) {
        answer.setText(message);
        footer.setText(" ");
    }

    /**
     * Asks the assistant to explain something.
     *
     * @param command what produced the result, recorded in the audit entry
     * @param prompt  what to send
     */
    public void explain(String command, AiPrompt prompt) {
        if (worker != null) {
            return;
        }
        if (ai.setupHint().isPresent()) {
            // Nothing is sent, so there is nothing to audit.
            answer.setText(ai.setupHint().get());
            footer.setText(" ");
            return;
        }
        sent.setText(prompt.describe());
        sent.setCaretPosition(0);
        answer.setText("");
        footer.setText("Asking " + ai.name() + "…");

        worker = new SwingWorker<>() {
            @Override
            protected AiAnswer doInBackground() throws Exception {
                return ai.ask(prompt, this::publish);
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(answer::append);
            }

            @Override
            protected void done() {
                worker = null;
                finished(command, prompt, this);
                onStateChange.run();
            }
        };
        worker.execute();
        onStateChange.run();
    }

    /** Stops the explanation in progress, if any. */
    public void cancel() {
        if (worker != null) {
            worker.cancel(true);
        }
    }

    private void finished(String command, AiPrompt prompt, SwingWorker<AiAnswer, String> done) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", command);
        details.put("assistant", ai.name());
        details.put("model", ai.model());
        details.put("promptChars", prompt.system().length() + prompt.user().length());
        try {
            AiAnswer result = done.get();
            NumberFormat n = NumberFormat.getIntegerInstance();
            footer.setText("Answered by " + result.model() + "  ·  " + n.format(result.inputTokens()) + " tokens in, "
                    + n.format(result.outputTokens()) + " out  ·  " + String.format("%.1f s", result.durationMs() / 1000.0));
            answer.setCaretPosition(0);
            details.put("outcome", "ok");
            details.put("answeredBy", result.model());
            details.put("inputTokens", result.inputTokens());
            details.put("outputTokens", result.outputTokens());
            details.put("durationMs", result.durationMs());
        } catch (CancellationException e) {
            footer.setText("Stopped.");
            details.put("outcome", "cancelled");
        } catch (ExecutionException e) {
            String message = e.getCause() instanceof AiException ai ? ai.getMessage() : String.valueOf(e.getCause());
            AiException.Kind kind = e.getCause() instanceof AiException ai ? ai.kind() : AiException.Kind.OTHER;
            answer.append((answer.getText().isEmpty() ? "" : "\n\n") + message);
            footer.setText(kind == AiException.Kind.CANCELLED ? "Stopped." : "No explanation: " + message);
            details.put("outcome", kind.name().toLowerCase());
            details.put("error", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            details.put("outcome", "cancelled");
        }
        log.record(source, "explain", details);
    }
}
