package com.dframework.dig;

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

import com.dframework.swing.ai.AiAnswer;
import com.dframework.swing.ai.AiAssistant;
import com.dframework.swing.ai.AiException;
import com.dframework.swing.ai.AiPrompt;
import com.dframework.swing.panel.DCollapsiblePane;
import com.dframework.swing.plugin.PluginContext;

/**
 * The "AI explanation" tab: the model's explanation of the last result,
 * written in as it arrives, with exactly what was sent available beneath.
 */
final class DigExplainPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient PluginContext context;
    private final transient AiAssistant ai;
    private final JTextArea answer = new JTextArea();
    private final JTextArea sent = new JTextArea();
    private final JLabel footer = new JLabel(" ");
    private final DCollapsiblePane sentPane;
    private transient SwingWorker<AiAnswer, String> worker;
    private transient Runnable onStateChange = () -> { };

    DigExplainPanel(PluginContext context, AiAssistant ai) {
        super(new BorderLayout(0, 6));
        this.context = context;
        this.ai = ai;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel disclosure = new JLabel(ai.name() + " (" + ai.model() + "). Explaining sends the dig command and its output to "
                + ai.provider() + ".");
        disclosure.setEnabled(false);
        add(disclosure, BorderLayout.NORTH);

        answer.setEditable(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        answer.setFont(answer.getFont().deriveFont(answer.getFont().getSize2D() + 1f));
        answer.setText(ai.setupHint().orElse("Run a lookup, then press \"Explain with AI\" to have "
                + ai.name() + " explain the result in plain English."));
        answer.getAccessibleContext().setAccessibleName("AI explanation");
        add(new JScrollPane(answer), BorderLayout.CENTER);

        sent.setEditable(false);
        sent.setLineWrap(true);
        sent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, sent.getFont().getSize()));
        sent.setRows(8);
        sentPane = new DCollapsiblePane("What was sent to " + ai.name(), new JScrollPane(sent), false);
        sentPane.rememberState(context.preferences(), "explain.sent.expanded");

        JPanel south = new JPanel(new BorderLayout(0, 4));
        footer.setEnabled(false);
        south.add(footer, BorderLayout.NORTH);
        south.add(sentPane, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    /** Called whenever an explanation starts or finishes, so buttons can update. */
    void setOnStateChange(Runnable onStateChange) {
        this.onStateChange = onStateChange;
    }

    boolean isRunning() {
        return worker != null;
    }

    /** Asks the assistant to explain a finished run. */
    void explain(String command, int exitCode, List<String> output) {
        if (worker != null) {
            return;
        }
        AiPrompt prompt;
        try {
            prompt = DigExplainPrompt.build(command, exitCode, output);
        } catch (IllegalArgumentException e) {
            answer.setText(e.getMessage());
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
    void cancel() {
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
        context.audit("explain", details);
    }
}
