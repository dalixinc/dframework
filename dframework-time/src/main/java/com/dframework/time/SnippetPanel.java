package com.dframework.time;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * One side of a lesson: a heading, the code (editable), Run and Reset, and the
 * output. The before side is marked in red, the after side in green.
 */
final class SnippetPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    static final Color PITFALL = new Color(0xD9534F);
    static final Color FIX = new Color(0x2DA44E);

    private final JLabel heading = new JLabel();
    private final CodeArea code = new CodeArea();
    private final JTextPane output = new JTextPane();
    private final JButton run = new JButton("Run");
    private final JButton reset = new JButton("Reset");
    private final JButton wholeFile = new JButton("Whole file…");
    private final JSplitPane split;

    SnippetPanel(boolean before, Runnable onRun, Runnable onReset, Runnable onWholeFile) {
        super(new BorderLayout(0, 4));
        Color accent = before ? PITFALL : FIX;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(2, 8, 2, 2)));
        heading.setForeground(accent);

        run.setToolTipText("Compile and run this code (Ctrl+Enter)");
        reset.setToolTipText("Put back the original code");
        wholeFile.setToolTipText("Show the whole source file that is compiled");
        run.addActionListener(e -> onRun.run());
        reset.addActionListener(e -> onReset.run());
        wholeFile.addActionListener(e -> onWholeFile.run());
        for (JButton b : new JButton[] {reset, wholeFile}) {
            b.putClientProperty("JButton.buttonType", "borderless");
        }
        code.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "runSnippet");
        code.getActionMap().put("runSnippet", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onRun.run();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 0));
        buttons.add(wholeFile);
        buttons.add(reset);
        buttons.add(run);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(heading, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);

        output.setEditable(false);
        CodeArea.letKeyThrough(output);
        JScrollPane codeScroll = new JScrollPane(code);
        JScrollPane outputScroll = new JScrollPane(output);
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codeScroll, outputScroll);
        split.setResizeWeight(0.55);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        setFontScale(1f);
    }

    void show(Lesson.Snippet snippet, String currentCode) {
        heading.setText(snippet.heading());
        code.setCode(currentCode);
        clearOutput();
    }

    String code() {
        return code.getText();
    }

    void setCode(String text) {
        code.setCode(text);
    }

    JComponent codeArea() {
        return code;
    }

    void setRunning(boolean running) {
        run.setEnabled(!running);
        run.setText(running ? "Running…" : "Run");
    }

    void setEdited(boolean edited) {
        reset.setVisible(edited);
    }

    void setRunEnabled(boolean enabled) {
        run.setEnabled(enabled);
    }

    /** Scales the fonts, for presenting. */
    void setFontScale(float scale) {
        Font base = new JLabel().getFont();
        heading.setFont(base.deriveFont(Font.BOLD, base.getSize2D() * scale * 1.1f));
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(13 * scale));
        code.setFont(mono);
        output.setFont(mono);
        Font button = base.deriveFont(base.getSize2D() * scale);
        for (JButton b : new JButton[] {run, reset, wholeFile}) {
            b.setFont(button);
        }
    }

    /** Moves the divider so the output shows in full, giving it at most 60% of the space. */
    private void fitOutput() {
        int total = split.getHeight() - split.getDividerSize();
        if (total <= 0) {
            return;
        }
        java.awt.Insets in = output.getInsets();
        java.awt.Insets scrollIn = ((JComponent) split.getBottomComponent()).getInsets();
        int lines = output.getDocument().getDefaultRootElement().getElementCount();
        int needed = lines * output.getFontMetrics(output.getFont()).getHeight() + in.top + in.bottom
                + scrollIn.top + scrollIn.bottom + 4;
        int outputHeight = Math.max((int) (total * (1 - split.getResizeWeight())), Math.min(needed, (int) (total * 0.6)));
        split.setDividerLocation(total - outputHeight);
    }

    void clearOutput() {
        output.setText("");
        split.setDividerLocation(split.getResizeWeight());
    }

    /** Shows a result: output, then warnings and errors, then timings, each in its own style. */
    void showResult(SnippetRunner.Result result) {
        output.setText("");
        StyledDocument doc = output.getStyledDocument();
        boolean dark = CodeArea.isDark(output.getBackground());
        Color warn = dark ? new Color(0xE5B567) : new Color(0x9A6700);
        Color error = dark ? new Color(0xFF7B72) : new Color(0xCF222E);
        Color dim = dark ? new Color(0x8B949E) : new Color(0x6E7781);
        for (String line : result.output()) {
            append(doc, line + "\n", output.getForeground(), false);
        }
        for (String w : result.warnings()) {
            append(doc, "compiler warning, " + w + "\n", warn, false);
        }
        for (String e : result.errors()) {
            String label = switch (result.status()) {
                case THREW -> "threw ";
                case COMPILE_ERROR -> "compile error, ";
                default -> "";
            };
            append(doc, label + e + "\n", error, true);
        }
        NumberFormat n = NumberFormat.getIntegerInstance();
        String timing = result.status() == SnippetRunner.Status.NO_COMPILER ? ""
                : "compiled in " + n.format(result.compileMs()) + " ms"
                        + (result.status() == SnippetRunner.Status.COMPILE_ERROR ? "" : ", ran in " + n.format(result.runMs()) + " ms");
        append(doc, timing, dim, false);
        output.setCaretPosition(0);
        javax.swing.SwingUtilities.invokeLater(this::fitOutput);
    }

    private static void append(StyledDocument doc, String text, Color colour, boolean bold) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, colour);
        StyleConstants.setBold(a, bold);
        try {
            doc.insertString(doc.getLength(), text, a);
        } catch (BadLocationException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
