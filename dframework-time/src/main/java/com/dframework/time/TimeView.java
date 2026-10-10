package com.dframework.time;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serial;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.dframework.swing.ai.DExplainPane;
import com.dframework.swing.button.DToggleSwitch;
import com.dframework.swing.exec.DCommandPane;
import com.dframework.swing.panel.DCollapsiblePane;
import com.dframework.swing.plugin.PluginContext;

/**
 * The Time in Java user interface: the era timeline, the list of lessons, and
 * the current lesson with its two examples, each compiled and run live.
 */
final class TimeView extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final float PRESENTER_SCALE = 1.45f;
    private static final int SIDE_BY_SIDE_WIDTH = 1050;

    private final transient PluginContext context;
    private final transient SnippetRunner runner = new SnippetRunner();
    private final transient List<Lesson> lessons = Lessons.all();
    private final boolean compilerAvailable = SnippetRunner.isAvailable();

    private final EraTimeline timeline = new EraTimeline();
    private final JList<Lesson> lessonList = new JList<>(lessons.toArray(new Lesson[0]));
    private final JScrollPane lessonScroll = new JScrollPane(lessonList);
    private final JLabel eraLabel = new JLabel();
    private final JLabel title = new JLabel();
    private final JTextArea story = new JTextArea();
    private final SnippetPanel before;
    private final SnippetPanel after;
    private final JPanel compare = new JPanel(new GridLayout(1, 2, 14, 0));
    private final JLabel takeaway = new JLabel();
    private final JButton previous = new JButton("◀  Previous");
    private final JButton next = new JButton("Next  ▶");
    private final JButton runBoth = new JButton("Run both");
    private final JLabel position = new JLabel();
    private final DToggleSwitch presenter = new DToggleSwitch("Presenter mode");
    private final JButton explain = new JButton("Explain with AI");
    private final JTabbedPane tabs = new JTabbedPane();
    private final transient DExplainPane explainPane;
    private final DCommandPane commandPane;
    private final DCollapsiblePane underTheHood;

    private int index = -1;
    /** The code on each side of each lesson, as edited; absent means the original. */
    private final transient Map<String, String> edits = new HashMap<>();
    /** The last result on each side of each lesson. */
    private final transient Map<String, SnippetRunner.Result> results = new HashMap<>();
    private final transient boolean[] running = new boolean[2];
    private transient LastRun lastRun;

    private record LastRun(Lesson lesson, String heading, String code, SnippetRunner.Result result) {
    }

    TimeView(PluginContext context) {
        super(new BorderLayout(0, 6));
        this.context = context;
        this.explainPane = context.ai()
                .map(ai -> new DExplainPane(ai, context.auditLog(), context.plugin().id(), context.preferences()))
                .orElse(null);
        this.commandPane = new DCommandPane(context.auditLog(), context.plugin().id(), "run");
        setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        before = new SnippetPanel(true, () -> run(0), () -> reset(0), () -> showWholeFile(0));
        after = new SnippetPanel(false, () -> run(1), () -> reset(1), () -> showWholeFile(1));

        timeline.setOnChoose(this::goToEra);
        add(timeline, BorderLayout.NORTH);

        lessonList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lessonList.setCellRenderer(new LessonRenderer());
        lessonList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && lessonList.getSelectedIndex() >= 0) {
                show(lessonList.getSelectedIndex());
            }
        });
        // Left and right are for the lessons, not the list's (non-existent) columns.
        lessonList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "none");
        lessonList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "none");
        lessonScroll.setPreferredSize(new Dimension(232, 200));
        add(lessonScroll, BorderLayout.WEST);

        JComponent lessonPanel = lessonPanel();
        tabs.addTab("Lesson", lessonPanel);
        if (explainPane != null) {
            tabs.addTab("AI explanation", explainPane);
            explainPane.setOnStateChange(this::updateButtons);
        }
        // With no AI assistant there'd be one lonely tab: show the lesson on its own.
        JComponent main = explainPane != null ? tabs : lessonPanel;
        // Collapsed at first: in a talk the examples are the point. Opened once, it stays open.
        underTheHood = new DCollapsiblePane("Under the hood", commandPane, false);
        underTheHood.rememberState(context.preferences(), "underTheHood.expanded");
        add(DCollapsiblePane.splitBelow(main, underTheHood), BorderLayout.CENTER);
        commandPane.setCaption("Each example is compiled in memory by the JDK's own compiler (javax.tools),"
                + " then its run(out) method is called. The equivalent command:");
        commandPane.setCommand(javacCommand());

        bindKeys();
        presenter.setSelected(context.preferences().getBoolean("presenter", false));
        presenter.addActionListener(e -> {
            context.preferences().putBoolean("presenter", presenter.isSelected());
            applyScale();
        });
        applyScale();
        int start = Math.max(0, Math.min(lessons.size() - 1, context.preferences().getInt("lesson", 0)));
        lessonList.setSelectedIndex(start);
        updateButtons();
    }

    // ---- layout -------------------------------------------------------------------------------

    private JComponent lessonPanel() {
        eraLabel.setEnabled(false);
        story.setEditable(false);
        story.setFocusable(false);
        story.setLineWrap(true);
        story.setWrapStyleWord(true);
        story.setOpaque(false);
        story.setBorder(BorderFactory.createEmptyBorder());

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(eraLabel, BorderLayout.NORTH);
        header.add(title, BorderLayout.CENTER);
        header.add(story, BorderLayout.SOUTH);

        compare.add(before);
        compare.add(after);
        // Side by side when there's room (a projector), one above the other when not.
        compare.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean wide = compare.getWidth() >= SIDE_BY_SIDE_WIDTH;
                GridLayout layout = (GridLayout) compare.getLayout();
                if (wide != (layout.getColumns() == 2)) {
                    compare.setLayout(wide ? new GridLayout(1, 2, 14, 0) : new GridLayout(2, 1, 0, 10));
                    compare.revalidate();
                }
            }
        });

        takeaway.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent()),
                BorderFactory.createEmptyBorder(4, 10, 4, 4)));

        previous.addActionListener(e -> step(-1));
        next.addActionListener(e -> step(1));
        runBoth.addActionListener(e -> runBoth());
        runBoth.setToolTipText("Run the old way, then the new way (Ctrl+Shift+Enter)");
        previous.setToolTipText("Previous lesson (Page Up)");
        next.setToolTipText("Next lesson (Page Down)");
        presenter.setToolTipText("Larger text, and the lesson list hidden, for a projector");
        explain.setVisible(explainPane != null);
        explain.setToolTipText("Have the AI assistant explain the last example run");
        explain.addActionListener(e -> explainLast());
        position.setEnabled(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        left.add(previous);
        left.add(position);
        left.add(next);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
        right.add(presenter);
        right.add(explain);
        right.add(runBoth);
        JPanel nav = new JPanel(new BorderLayout());
        nav.add(left, BorderLayout.WEST);
        nav.add(right, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(takeaway, BorderLayout.NORTH);
        footer.add(nav, BorderLayout.SOUTH);

        LessonPanel panel = new LessonPanel(header, footer);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 4));
        panel.add(header, BorderLayout.NORTH);
        panel.add(compare, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * The lesson: it fills the space it's given, but never squeezes the examples
     * below a usable size. In a small window it scrolls instead.
     */
    private final class LessonPanel extends JPanel implements Scrollable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** The least height for one example: heading, a few lines of code, a few of output. */
        private static final int EXAMPLE_HEIGHT = 250;

        private final JComponent header;
        private final JComponent footer;

        LessonPanel(JComponent header, JComponent footer) {
            super(new BorderLayout(0, 10));
            this.header = header;
            this.footer = footer;
        }

        /** The height below which the examples would be squeezed. */
        private int neededHeight() {
            boolean stacked = ((GridLayout) compare.getLayout()).getRows() == 2;
            int examples = Math.round((stacked ? 2 * EXAMPLE_HEIGHT + 10 : EXAMPLE_HEIGHT) * scale());
            java.awt.Insets in = getInsets();
            return in.top + in.bottom + header.getPreferredSize().height + footer.getPreferredSize().height
                    + examples + 20;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, neededHeight());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return visible.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() == null || getParent().getHeight() >= neededHeight();
        }
    }

    private void bindKeys() {
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "nextLesson", () -> step(1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "previousLesson", () -> step(-1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextLesson", () -> step(1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousLesson", () -> step(-1));
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "runBoth", this::runBoth);
        DocumentListener edited = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateEdited();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateEdited();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // styling only
            }
        };
        ((javax.swing.text.JTextComponent) before.codeArea()).getDocument().addDocumentListener(edited);
        ((javax.swing.text.JTextComponent) after.codeArea()).getDocument().addDocumentListener(edited);
    }

    private void bind(KeyStroke key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        getActionMap().put(name, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---- navigation ---------------------------------------------------------------------------

    private void step(int delta) {
        int target = index + delta;
        if (target >= 0 && target < lessons.size()) {
            lessonList.setSelectedIndex(target);
            lessonList.ensureIndexIsVisible(target);
        }
    }

    private void goToEra(Lesson.Era era) {
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).era() == era) {
                lessonList.setSelectedIndex(i);
                lessonList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void show(int newIndex) {
        if (newIndex == index) {
            return;
        }
        saveEdits();
        index = newIndex;
        Lesson lesson = lessons.get(index);
        context.preferences().putInt("lesson", index);

        timeline.setCurrent(lesson.era());
        eraLabel.setText(lesson.era().year() + "  ·  " + lesson.era().label() + "  ·  " + lesson.era().summary());
        title.setText((index + 1) + ".  " + lesson.title());
        story.setText(lesson.story());
        takeaway.setText("<html><b>Takeaway:</b> " + escape(lesson.takeaway()) + "</html>");
        position.setText((index + 1) + " of " + lessons.size());
        before.show(lesson.before(), codeFor(index, 0));
        after.show(lesson.after(), codeFor(index, 1));
        showCachedResult(0, before);
        showCachedResult(1, after);
        updateEdited();
        updateButtons();
        if (tabs.getTabCount() > 0 && explainPane != null) {
            tabs.setSelectedIndex(0);
        }
    }

    private void showCachedResult(int side, SnippetPanel panel) {
        SnippetRunner.Result cached = results.get(key(index, side));
        if (cached != null) {
            panel.showResult(cached);
        }
    }

    private void saveEdits() {
        if (index < 0) {
            return;
        }
        saveEdit(0, before.code());
        saveEdit(1, after.code());
    }

    private void saveEdit(int side, String code) {
        if (code.equals(original(index, side))) {
            edits.remove(key(index, side));
        } else {
            edits.put(key(index, side), code);
        }
    }

    private String codeFor(int lesson, int side) {
        return edits.getOrDefault(key(lesson, side), original(lesson, side));
    }

    private String original(int lesson, int side) {
        Lesson l = lessons.get(lesson);
        return side == 0 ? l.before().code() : l.after().code();
    }

    private static String key(int lesson, int side) {
        return lesson + "/" + side;
    }

    private void updateEdited() {
        if (index < 0) {
            return;
        }
        before.setEdited(!before.code().equals(original(index, 0)));
        after.setEdited(!after.code().equals(original(index, 1)));
    }

    private void reset(int side) {
        (side == 0 ? before : after).setCode(original(index, side));
        edits.remove(key(index, side));
        updateEdited();
    }

    // ---- running ------------------------------------------------------------------------------

    private void runBoth() {
        run(0, () -> run(1));
    }

    private void run(int side) {
        run(side, () -> { });
    }

    /** Compiles and runs one side in the background, shows and audits the result, then calls {@code then}. */
    private void run(int side, Runnable then) {
        if (running[side] || index < 0) {
            return;
        }
        SnippetPanel panel = side == 0 ? before : after;
        Lesson lesson = lessons.get(index);
        Lesson.Snippet snippet = side == 0 ? lesson.before() : lesson.after();
        String code = panel.code();
        boolean edited = !code.equals(snippet.code());
        int lessonIndex = index;
        running[side] = true;
        panel.setRunning(true);
        updateButtons();
        new SwingWorker<SnippetRunner.Result, Void>() {
            @Override
            protected SnippetRunner.Result doInBackground() {
                return runner.run(code);
            }

            @Override
            protected void done() {
                running[side] = false;
                panel.setRunning(false);
                try {
                    SnippetRunner.Result result = get();
                    results.put(key(lessonIndex, side), result);
                    if (lessonIndex == index) {
                        panel.showResult(result);
                    }
                    audit(lessonIndex, lesson, snippet, edited, result);
                    lastRun = new LastRun(lesson, snippet.heading(), code, result);
                } catch (ExecutionException e) {
                    panel.clearOutput();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                updateButtons();
                if (lessonIndex == index) {
                    then.run();
                }
            }
        }.execute();
    }

    private void audit(int lessonIndex, Lesson lesson, Lesson.Snippet snippet, boolean edited, SnippetRunner.Result r) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", (lessonIndex + 1) + ". " + lesson.title() + " › " + snippet.heading());
        details.put("lesson", lesson.title());
        details.put("example", snippet.heading());
        details.put("edited", edited);
        details.put("outcome", switch (r.status()) {
            case OK -> "ok";
            case THREW -> "threw";
            case COMPILE_ERROR -> "compile error";
            case TIMED_OUT -> "timed out";
            case NO_COMPILER -> "no compiler";
        });
        details.put("durationMs", r.compileMs() + r.runMs());
        details.put("compileMs", r.compileMs());
        details.put("runMs", r.runMs());
        details.put("outputLines", r.output().size());
        if (!r.errors().isEmpty()) {
            details.put("error", r.errors().get(0));
        }
        context.audit("run", details);
    }

    /** Compiles a trivial snippet in the background, so the first Run in front of an audience is quick. */
    void warmUp() {
        if (!compilerAvailable) {
            return;
        }
        Thread warm = new Thread(() -> runner.run("out.print(\"\");"), "time plugin warm-up");
        warm.setDaemon(true);
        warm.start();
    }

    private void showWholeFile(int side) {
        if (index < 0) {
            return;
        }
        Lesson lesson = lessons.get(index);
        String heading = side == 0 ? lesson.before().heading() : lesson.after().heading();
        String code = (side == 0 ? before : after).code();
        JTextArea text = new JTextArea("// " + javacCommand() + "\n// then Snippet.run(out), where out prints to the panel\n\n"
                + SnippetRunner.compilationUnit("Snippet", code));
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(13 * scale())));
        text.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(Math.round(760 * scale()), Math.round(460 * scale())));
        JOptionPane.showMessageDialog(this, scroll, "Whole file: " + heading, JOptionPane.PLAIN_MESSAGE);
    }

    /** The command a terminal user would type to do what the plugin does in memory. */
    String javacCommand() {
        StringBuilder names = new StringBuilder();
        for (String entry : runner.classPath().split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                Path p = Path.of(entry);
                names.append(names.isEmpty() ? "" : File.pathSeparator)
                        .append(p.getFileName() != null ? p.getFileName() : p);
            }
        }
        return "javac " + String.join(" ", SnippetRunner.OPTIONS) + " -classpath " + names + " Snippet.java";
    }

    // ---- AI -----------------------------------------------------------------------------------

    void explainLast() {
        if (explainPane == null || explainPane.isRunning()) {
            return;
        }
        tabs.setSelectedComponent(explainPane);
        if (lastRun == null) {
            explainPane.showMessage("Run an example first, then ask for an explanation.");
            return;
        }
        try {
            explainPane.explain(lastRun.lesson().title() + " › " + lastRun.heading(),
                    TimeExplainPrompt.build(lastRun.lesson(), lastRun.heading(), lastRun.code(), lastRun.result()));
        } catch (IllegalArgumentException tooLong) {
            explainPane.showMessage(tooLong.getMessage());
        }
    }

    boolean canExplain() {
        return explainPane != null;
    }

    // ---- state --------------------------------------------------------------------------------

    private void updateButtons() {
        boolean explaining = explainPane != null && explainPane.isRunning();
        before.setRunEnabled(compilerAvailable && !running[0]);
        after.setRunEnabled(compilerAvailable && !running[1]);
        runBoth.setEnabled(compilerAvailable && !running[0] && !running[1]);
        previous.setEnabled(index > 0);
        next.setEnabled(index < lessons.size() - 1);
        explain.setEnabled(lastRun != null && !explaining);
        if (!compilerAvailable) {
            runBoth.setToolTipText("No Java compiler: run on a JDK, not a JRE, to run the examples");
        }
    }

    private float scale() {
        return presenter.isSelected() ? PRESENTER_SCALE : 1f;
    }

    /** Applies presenter mode: bigger text everywhere, and the lesson list hidden. */
    private void applyScale() {
        float scale = scale();
        Font base = UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font") : getFont();
        float size = base.getSize2D();
        title.setFont(base.deriveFont(Font.BOLD, size * 1.55f * scale));
        eraLabel.setFont(base.deriveFont(size * scale));
        story.setFont(base.deriveFont(size * 1.08f * scale));
        takeaway.setFont(base.deriveFont(size * 1.08f * scale));
        timeline.setScale(scale);
        Font buttons = base.deriveFont(size * scale);
        for (JComponent c : new JComponent[] {previous, next, runBoth, explain, position, presenter}) {
            c.setFont(buttons);
        }
        before.setFontScale(scale);
        after.setFontScale(scale);
        lessonScroll.setVisible(!presenter.isSelected());
        revalidate();
        repaint();
    }

    void onShow() {
        next.requestFocusInWindow();
        if (index >= 0) {
            lessonList.ensureIndexIsVisible(index);
        }
    }

    void togglePresenterMode() {
        presenter.setSelected(!presenter.isSelected());
        context.preferences().putBoolean("presenter", presenter.isSelected());
        applyScale();
    }

    void nextLesson() {
        step(1);
    }

    void previousLesson() {
        step(-1);
    }

    void runBothExamples() {
        runBoth();
    }

    void toggleUnderTheHood() {
        underTheHood.setExpanded(!underTheHood.isExpanded());
    }

    void dispose() {
        if (explainPane != null) {
            explainPane.cancel();
        }
        commandPane.dispose();
    }

    private static java.awt.Color accent() {
        java.awt.Color c = UIManager.getColor("Component.accentColor");
        return c != null ? c : new java.awt.Color(0x3B82F6);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Lesson rows, with the era as a small heading above each era's first lesson. */
    private final class LessonRenderer extends DefaultListCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int i, boolean selected,
                boolean focused) {
            super.getListCellRendererComponent(list, value, i, selected, focused);
            Lesson lesson = (Lesson) value;
            boolean firstOfEra = i == 0 || lessons.get(i - 1).era() != lesson.era();
            String grey = selected ? "" : " color='gray'";
            String era = firstOfEra
                    ? "<div style='margin-top:6px'><font size='-1'" + grey + "><b>" + escape(lesson.era().year()
                            + "  " + lesson.era().label()) + "</b></font></div>"
                    : "";
            setText("<html>" + era + "<div style='margin-left:8px'>" + (i + 1) + ". " + escape(lesson.title())
                    + "</div></html>");
            setToolTipText(lesson.story());
            return this;
        }
    }

    /** The lesson showing, counting from 0. */
    int currentIndex() {
        return index;
    }
}
