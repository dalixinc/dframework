package com.dframework.git;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import com.dframework.swing.ai.DExplainPane;
import com.dframework.swing.exec.CommandLines;
import com.dframework.swing.exec.CommandRunner;
import com.dframework.swing.exec.DCommandPane;
import com.dframework.swing.field.DTextField;
import com.dframework.swing.graph.DCommitGraph;
import com.dframework.swing.graph.GraphCommit;
import com.dframework.swing.nav.DBreadcrumbBar;
import com.dframework.swing.nav.DBreadcrumbBar.Crumb;
import com.dframework.swing.panel.DCollapsiblePane;
import com.dframework.swing.plugin.PluginContext;
import com.dframework.swing.table.DTable;

/** The Git Browser user interface. */
final class GitView extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color GOOD = new Color(0x27AE60);
    private static final Color BAD = new Color(0xE0443E);
    private static final long LARGEST_BLOB_SHOWN = 2_000_000;

    private final transient PluginContext context;
    private final boolean gitAvailable;

    // Repository
    private final DTextField repositoryField = new DTextField(40);
    private final JLabel repositoryInfo = new JLabel("No repository open");
    private transient Path repository;

    // Commands tab
    private final JComboBox<GitPreset> preset = new JComboBox<>(GitPreset.values());
    private final DTextField value = new DTextField(18);
    private final JLabel valueLabel = new JLabel("Revision");
    private final JSpinner limit = new JSpinner(new SpinnerNumberModel(50, 1, 10_000, 10));
    private final JLabel description = new JLabel(" ");
    private final JButton run = new JButton("Run");
    private final JButton stop = new JButton("Stop");
    private final JLabel status = new JLabel(" ");
    private final JTextArea output = new JTextArea();

    // Objects tab
    private final DTextField object = new DTextField(30);
    private final JButton inspect = new JButton("Inspect");
    private final DBreadcrumbBar trail = new DBreadcrumbBar();
    private final JLabel objectHeader = new JLabel(" ");
    private final JPanel links = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
    private final JTextArea objectText = new JTextArea();
    private final TreeModel treeModel = new TreeModel();
    private final DTable tree = new DTable(treeModel);
    private final CardLayout objectCards = new CardLayout();
    private final JPanel objectContent = new JPanel(objectCards);

    // Graph tab
    private final DCommitGraph graph = new DCommitGraph();
    private final JCheckBox allBranches = new JCheckBox("All branches", true);
    private final JSpinner graphLimit = new JSpinner(new SpinnerNumberModel(500, 1, 10_000, 100));
    private final JButton refreshGraph = new JButton("Refresh");
    private final JLabel graphStatus = new JLabel(" ");
    private transient Path graphShows;   // the repository the graph was last drawn for

    private final JTabbedPane tabs = new JTabbedPane();
    private final JComponent commandsTab;
    private final JComponent graphTab;
    private final JComponent objectsTab;
    private final JButton explain = new JButton("Explain with AI");
    private final transient DExplainPane explainPane;
    private final DCommandPane commandPane;
    private final DCollapsiblePane underTheHood;

    private transient CommandRunner runner;
    private transient boolean busy;
    private transient LastOutput lastOutput;

    /** The most recent output, for explaining. */
    private record LastOutput(String command, int exitCode, List<String> lines) {
    }

    GitView(PluginContext context) {
        super(new BorderLayout(0, 8));
        this.context = context;
        this.gitAvailable = CommandRunner.locate(GitCommands.EXECUTABLE).isPresent();
        this.explainPane = context.ai()
                .map(ai -> new DExplainPane(ai, context.auditLog(), context.plugin().id(), context.preferences()))
                .orElse(null);
        this.commandPane = new DCommandPane(context.auditLog(), context.plugin().id(), "open", "run", "graph", "inspect");
        setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        add(repositoryBar(), BorderLayout.NORTH);
        commandsTab = commandsTab();
        graphTab = graphTab();
        objectsTab = objectsTab();
        tabs.addTab("Commands", commandsTab);
        tabs.addTab("Graph", graphTab);
        tabs.addTab("Objects", objectsTab);
        if (explainPane != null) {
            tabs.addTab("AI explanation", explainPane);
            explainPane.setOnStateChange(this::updateButtons);
        }
        tabs.addChangeListener(e -> {
            updatePreview();
            loadGraphIfNeeded();
        });
        add(tabs, BorderLayout.CENTER);

        underTheHood = new DCollapsiblePane("Under the hood", commandPane);
        underTheHood.rememberState(context.preferences(), "underTheHood.expanded");
        // A split, so the details never squeeze the results; collapsing gives the room back.
        remove(tabs);
        add(DCollapsiblePane.splitBelow(tabs, underTheHood), BorderLayout.CENTER);

        restoreSettings();
        wireUp();
        presetChanged();
        updateButtons();
    }

    // ---- layout -----------------------------------------------------------------------------

    private JComponent repositoryBar() {
        repositoryField.setValidator(text -> text.isBlank() ? Optional.of("Choose a folder") : Optional.empty());
        repositoryField.putClientProperty("JTextField.placeholderText", "a folder inside a git repository");
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> browse());
        JButton open = new JButton("Open");
        open.addActionListener(e -> openRepository());
        repositoryField.addActionListener(e -> openRepository());
        explain.setVisible(explainPane != null);
        explain.setToolTipText("Have the AI assistant explain the last output");
        repositoryInfo.setEnabled(false);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel("Repository"), BorderLayout.WEST);
        row.add(repositoryField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        buttons.add(browse);
        buttons.add(open);
        buttons.add(explain);
        row.add(buttons, BorderLayout.EAST);

        JPanel bar = new JPanel(new BorderLayout(0, 4));
        if (!gitAvailable) {
            JLabel missing = new JLabel("<html><b>git was not found on this computer's PATH.</b> Install it"
                    + " (<code>sudo apt install git</code> on Debian and Ubuntu), then reopen this tool.</html>");
            missing.setForeground(BAD);
            bar.add(missing, BorderLayout.NORTH);
        }
        bar.add(row, BorderLayout.CENTER);
        bar.add(repositoryInfo, BorderLayout.SOUTH);
        return bar;
    }

    private JComponent commandsTab() {
        value.setColumns(18);
        description.setEnabled(false);
        stop.setEnabled(false);
        output.setEditable(false);
        output.setFont(monospace(output.getFont()));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        form.add(new JLabel("Command"), c);
        form.add(preset, c);
        form.add(valueLabel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(value, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Limit"), c);
        form.add(limit, c);
        form.add(run, c);
        form.add(stop, c);
        c.gridy = 1;
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        form.add(description, c);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(form, BorderLayout.CENTER);
        top.add(status, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout(0, 6));
        tab.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        tab.add(top, BorderLayout.NORTH);
        tab.add(new JScrollPane(output), BorderLayout.CENTER);
        return tab;
    }

    private JComponent graphTab() {
        graph.setToolTipText("");   // tooltips come from DCommitGraph.getToolTipText
        graph.addOpenListener(commit -> {
            object.setText(commit.id());
            trail.setPath(Crumb.of(commit.id().substring(0, Math.min(7, commit.id().length())), commit.id()));
            inspect(commit.id(), false);
        });
        allBranches.setToolTipText("Include every branch and tag, not just the current branch's history");
        graphLimit.setToolTipText("How many commits to draw, newest first");
        refreshGraph.setToolTipText("Run git log again");
        allBranches.addActionListener(e -> {
            updatePreview();
            loadGraph();
        });
        refreshGraph.addActionListener(e -> loadGraph());

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        form.add(allBranches);
        form.add(new JLabel("Limit"));
        form.add(graphLimit);
        form.add(refreshGraph);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(form, BorderLayout.CENTER);
        top.add(graphStatus, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout(0, 6));
        tab.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        tab.add(top, BorderLayout.NORTH);
        tab.add(new JScrollPane(graph), BorderLayout.CENTER);
        return tab;
    }

    private JComponent objectsTab() {
        object.setInputFilter(GitInput.objectFilter());
        object.setValidator(GitInput.objectValidator());
        object.putClientProperty("JTextField.placeholderText", "HEAD, HEAD~3, HEAD:README.md, main^{tree} or an object id");
        objectHeader.setFont(objectHeader.getFont().deriveFont(Font.BOLD));
        objectText.setEditable(false);
        objectText.setFont(monospace(objectText.getFont()));
        tree.setEmptyText("This tree is empty.");
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tree.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && row >= 0) {
                    GitObjects.TreeEntry entry = treeModel.entry(tree.convertRowIndexToModel(row));
                    follow(entry.name(), entry.id());
                }
            }
        });
        tree.setToolTipText("Double-click an entry to inspect it");
        objectContent.add(new JScrollPane(objectText), "text");
        objectContent.add(new JScrollPane(tree), "tree");

        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.add(new JLabel("Object"), BorderLayout.WEST);
        form.add(object, BorderLayout.CENTER);
        form.add(inspect, BorderLayout.EAST);

        trail.addCrumbListener(crumb -> inspect((String) crumb.value(), false));
        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.add(form, BorderLayout.NORTH);
        head.add(trail, BorderLayout.CENTER);
        JPanel summary = new JPanel(new BorderLayout(0, 2));
        summary.add(objectHeader, BorderLayout.NORTH);
        summary.add(links, BorderLayout.CENTER);
        head.add(summary, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout(0, 6));
        tab.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        tab.add(head, BorderLayout.NORTH);
        tab.add(objectContent, BorderLayout.CENTER);
        showMessage("Inspect an object to see how git stores it. Commits point to a tree and their parents;"
                + " trees list files (blobs) and folders (trees). Try HEAD, then follow the links.");
        return tab;
    }

    private void wireUp() {
        DocumentListener preview = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        };
        repositoryField.getDocument().addDocumentListener(preview);
        value.getDocument().addDocumentListener(preview);
        object.getDocument().addDocumentListener(preview);
        limit.addChangeListener(e -> updatePreview());
        graphLimit.addChangeListener(e -> updatePreview());
        preset.addActionListener(e -> presetChanged());
        run.addActionListener(e -> runPreset());
        value.addActionListener(e -> runPreset());
        stop.addActionListener(e -> cancel());
        inspect.addActionListener(e -> inspect(object.getText().strip(), true));
        object.addActionListener(e -> inspect(object.getText().strip(), true));
        explain.addActionListener(e -> explainLast());
    }

    // ---- repository ---------------------------------------------------------------------------

    private void browse() {
        JFileChooser chooser = new JFileChooser(repositoryField.getText().isBlank() ? null : repositoryField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose a folder in a git repository");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            repositoryField.setText(chooser.getSelectedFile().getPath());
            openRepository();
        }
    }

    /** Opens the repository containing the folder in the field. */
    void openRepository() {
        if (busy || !gitAvailable || !repositoryField.validateInput()) {
            return;
        }
        openRepository(true);
    }

    /**
     * @param complain whether a folder that isn't in a repository is an error
     *                 (when the user asked) or just a prompt (when opening by default)
     */
    private void openRepository(boolean complain) {
        Path folder = Path.of(repositoryField.getText().strip()).toAbsolutePath().normalize();
        if (!Files.isDirectory(folder)) {
            setRepositoryInfo(BAD, "That folder doesn't exist: " + folder);
            return;
        }
        execute(List.of(new Step("open", GitCommands.topLevel(folder))), outcomes -> {
            CommandRunner.Outcome top = outcomes.get(0);
            if (top.exitCode() != 0 || top.lines().isEmpty()) {
                repository = null;
                if (complain) {
                    setRepositoryInfo(BAD, "Not a git repository: " + lastLine(top));
                } else {
                    repositoryField.setText("");
                    repositoryInfo.setForeground(status.getForeground());
                    repositoryInfo.setEnabled(false);
                    repositoryInfo.setText("Choose a folder inside a git repository, then press Open.");
                }
                updatePreview();
                return;
            }
            Path root = Path.of(top.lines().get(0).strip());
            execute(List.of(new Step("open", GitCommands.currentBranch(root))), more -> {
                repository = root;
                repositoryField.setText(root.toString());
                String branch = more.get(0).exitCode() == 0 && !more.get(0).lines().isEmpty() ? more.get(0).lines().get(0) : "?";
                setRepositoryInfo(GOOD, "Open: " + root + "   ·   branch " + branch);
                context.preferences().put("repository", root.toString());
                updatePreview();
                updateButtons();
                loadGraphIfNeeded();
            });
        });
    }

    private void setRepositoryInfo(Color colour, String text) {
        repositoryInfo.setEnabled(true);
        repositoryInfo.setForeground(colour);
        repositoryInfo.setText(text);
    }

    /**
     * The first time the plugin is shown, opens the remembered repository, or
     * else the one containing the folder the application started in. Git finds
     * the repository from any folder inside it, so starting in a subfolder (as
     * "gradlew run" does) works too.
     */
    void openDefaultRepository() {
        if (repository != null || busy || !gitAvailable) {
            return;
        }
        boolean remembered = !repositoryField.getText().isBlank();
        if (!remembered) {
            repositoryField.setText(System.getProperty("user.dir"));
        }
        openRepository(remembered);
    }

    // ---- commands tab -----------------------------------------------------------------------

    private void presetChanged() {
        GitPreset p = (GitPreset) preset.getSelectedItem();
        description.setText(p.description());
        boolean needsValue = p.input() != GitPreset.Input.NONE;
        value.setEnabled(needsValue);
        valueLabel.setEnabled(needsValue);
        valueLabel.setText(p.input() == GitPreset.Input.PATH ? "Path" : "Revision");
        if (p.input() == GitPreset.Input.PATH) {
            value.setInputFilter(GitInput.pathFilter());
            value.setValidator(GitInput.pathValidator());
            value.putClientProperty("JTextField.placeholderText", "a file, e.g. README.md");
        } else if (p.input() == GitPreset.Input.REVISION) {
            value.setInputFilter(GitInput.revisionFilter());
            value.setValidator(text -> text.isBlank() ? Optional.empty() : GitInput.revisionValidator().validate(text));
            value.putClientProperty("JTextField.placeholderText", "HEAD");
        } else {
            value.setInputFilter(null);
            value.setValidator(null);
            value.putClientProperty("JTextField.placeholderText", "not needed");
        }
        value.resetValidation();
        limit.setEnabled(p.usesLimit());
        updatePreview();
    }

    /** Runs the chosen preset. */
    void runPreset() {
        if (busy || !gitAvailable) {
            return;
        }
        if (repository == null) {
            setStatus(BAD, "Open a repository first.");
            return;
        }
        GitPreset p = (GitPreset) preset.getSelectedItem();
        if (p.input() == GitPreset.Input.PATH && !value.validateInput()) {
            return;
        }
        List<String> command;
        try {
            command = p.command(repository, value.getText(), (Integer) limit.getValue());
        } catch (IllegalArgumentException invalid) {
            value.validateInput();
            setStatus(BAD, invalid.getMessage());
            return;
        }
        String display = display(command);
        output.setText("$ " + display + "\n\n");
        setStatus(status.getForeground(), "Running…");
        tabs.setSelectedComponent(commandsTab);
        execute(List.of(new Step("run", command, p.label(), output)), outcomes -> {
            CommandRunner.Outcome outcome = outcomes.get(0);
            lastOutput = new LastOutput(display, outcome.exitCode(), outcome.lines());
            output.setCaretPosition(0);
            if (outcome.cancelled()) {
                setStatus(BAD, "Stopped after " + outcome.durationMs() + " ms.");
            } else if (outcome.exitCode() != 0) {
                setStatus(BAD, "git reported an error (exit code " + outcome.exitCode() + "): " + lastLine(outcome));
            } else {
                Optional<String> note = p.note(outcome.lines());
                note.ifPresent(text -> output.append((outcome.lines().isEmpty() ? "" : "\n") + "(" + text + ")\n"));
                setStatus(GOOD, p.label() + "  ·  " + note.orElse(count(outcome.lines().size(), "line"))
                        + "  ·  " + outcome.durationMs() + " ms" + (outcome.truncated()
                                ? "  ·  output stopped at " + count(outcome.lines().size(), "line") : ""));
            }
        });
    }

    // ---- graph tab ----------------------------------------------------------------------------

    /** Draws the graph when its tab is showing and it doesn't already show the open repository. */
    private void loadGraphIfNeeded() {
        if (tabs.getSelectedComponent() == graphTab && repository != null && !repository.equals(graphShows)) {
            loadGraph();
        }
    }

    /** Runs git log and draws the commit graph. */
    void loadGraph() {
        if (busy || !gitAvailable) {
            return;
        }
        if (repository == null) {
            setGraphStatus(BAD, "Open a repository first.");
            return;
        }
        int max = (Integer) graphLimit.getValue();
        List<String> command = GitLog.command(repository, allBranches.isSelected(), max);
        Path repo = repository;
        setGraphStatus(status.getForeground(), "Reading the history…");
        execute(List.of(new Step("graph", command, "Commit graph", null)), outcomes -> {
            CommandRunner.Outcome outcome = outcomes.get(0);
            graphShows = repo;
            if (outcome.cancelled()) {
                setGraphStatus(BAD, "Stopped after " + outcome.durationMs() + " ms.");
                return;
            }
            if (outcome.exitCode() != 0) {
                graph.setCommits(List.of());
                boolean empty = outcome.lines().stream().anyMatch(l -> l.contains("does not have any commits"));
                setGraphStatus(empty ? status.getForeground() : BAD, empty ? "No commits yet."
                        : "git reported an error (exit code " + outcome.exitCode() + "): " + lastLine(outcome));
                return;
            }
            List<GraphCommit> commits = GitLog.parse(outcome.lines());
            graph.setCommits(commits);
            long merges = commits.stream().filter(GraphCommit::isMerge).count();
            setGraphStatus(GOOD, count(commits.size(), "commit") + "  ·  " + count((int) merges, "merge")
                    + "  ·  " + outcome.durationMs() + " ms"
                    + (commits.size() >= max ? "  ·  limit reached: raise Limit to see older commits" : "")
                    + "  ·  double-click a commit to inspect it");
            lastOutput = new LastOutput(display(command), outcome.exitCode(), describe(commits));
            updateButtons();
        });
    }

    /** The graph as readable lines, for the AI explanation (not the raw separator-delimited output). */
    private static List<String> describe(List<GraphCommit> commits) {
        List<String> lines = new ArrayList<>();
        for (GraphCommit c : commits) {
            String refs = c.refs().isEmpty() ? ""
                    : " (" + String.join(", ", c.refs().stream().map(r -> r.kind() == GraphCommit.Kind.TAG ? "tag: " + r.name()
                            : r.kind() == GraphCommit.Kind.HEAD ? "HEAD -> " + r.name() : r.name()).toList()) + ")";
            String parents = c.parents().stream().map(id -> id.substring(0, Math.min(7, id.length())))
                    .reduce((a, b) -> a + " " + b).orElse("none");
            lines.add(c.id().substring(0, Math.min(7, c.id().length())) + refs + " " + c.subject()
                    + "  [parents: " + parents + "; " + c.author() + "]");
        }
        return lines;
    }

    private void setGraphStatus(Color colour, String text) {
        graphStatus.setForeground(colour);
        graphStatus.setText(text);
    }

    // ---- objects tab ------------------------------------------------------------------------

    /** Inspects an object followed from a tree entry or link, adding it to the trail. */
    private void follow(String label, String id) {
        trail.push(Crumb.of(label, id));
        inspect(id, false);
    }

    /**
     * Shows one object: resolves it to an id, then asks git for its type, size
     * and content (four commands, each shown and audited).
     *
     * @param newTrail whether to start a new breadcrumb trail (when typed in)
     */
    void inspect(String spec, boolean newTrail) {
        if (busy || !gitAvailable) {
            return;
        }
        if (repository == null) {
            objectHeader.setText("Open a repository first.");
            return;
        }
        if (newTrail) {
            if (!object.validateInput()) {
                return;
            }
            trail.setPath(Crumb.of(spec, spec));
        }
        tabs.setSelectedComponent(objectsTab);
        objectHeader.setText("Inspecting " + spec + "…");
        links.removeAll();
        Path repo = repository;
        execute(List.of(
                new Step("inspect", GitCommands.resolve(repo, spec)),
                new Step("inspect", GitCommands.catFile(repo, "-t", spec)),
                new Step("inspect", GitCommands.catFile(repo, "-s", spec))), first -> {
                    CommandRunner.Outcome resolved = first.get(0);
                    CommandRunner.Outcome type = first.get(1);
                    CommandRunner.Outcome size = first.get(2);
                    if (resolved.exitCode() != 0 || type.exitCode() != 0) {
                        objectHeader.setForeground(BAD);
                        objectHeader.setText("No such object: " + spec + "  (" + lastLine(type) + ")");
                        showMessage(String.join("\n", type.lines()));
                        return;
                    }
                    String id = resolved.lines().get(0).strip();
                    String kind = type.lines().get(0).strip();
                    long bytes = size.exitCode() == 0 ? Long.parseLong(size.lines().get(0).strip()) : -1;
                    objectHeader.setForeground(status.getForeground());
                    objectHeader.setText(kind + "   " + id + "   ·   " + NumberFormat.getIntegerInstance().format(bytes) + " bytes");
                    if (kind.equals("blob") && bytes > LARGEST_BLOB_SHOWN) {
                        showMessage("This file is " + NumberFormat.getIntegerInstance().format(bytes)
                                + " bytes, too large to show here. Its id is " + id + ".");
                        return;
                    }
                    List<String> pretty = GitCommands.catFile(repo, "-p", id);
                    execute(List.of(new Step("inspect", pretty)), second -> show(kind, id, display(pretty), second.get(0)));
                });
    }

    /** Prose in the object area: wrapped, and scrolled to the start. */
    private void showMessage(String text) {
        objectText.setLineWrap(true);
        objectText.setWrapStyleWord(true);
        objectText.setText(text);
        objectText.setCaretPosition(0);
        objectCards.show(objectContent, "text");
    }

    /** Object content (code, a commit): not wrapped, scrolled to the start. */
    private void showContent(List<String> lines) {
        objectText.setLineWrap(false);
        objectText.setText(String.join("\n", lines));
        objectText.setCaretPosition(0);
        objectCards.show(objectContent, "text");
    }

    private void show(String kind, String id, String command, CommandRunner.Outcome content) {
        lastOutput = new LastOutput(command, content.exitCode(), content.lines());
        links.removeAll();
        switch (kind) {
            case "tree" -> {
                treeModel.setEntries(GitObjects.parseTree(content.lines()));
                tree.packColumns(400);
                objectCards.show(objectContent, "tree");
            }
            case "blob" -> {
                if (GitObjects.looksBinary(content.lines())) {
                    showMessage("This is a binary file, so it isn't shown as text. Its id is " + id + ".");
                    lastOutput = null;
                } else {
                    showContent(content.lines());
                }
            }
            default -> {   // commit or tag
                showContent(content.lines());
                for (GitObjects.Link link : GitObjects.parseLinks(content.lines())) {
                    JButton go = new JButton(link.kind() + " " + link.id().substring(0, 10) + "…");
                    go.setToolTipText("Inspect this " + link.kind() + " (" + link.id() + ")");
                    go.addActionListener(e -> follow(link.kind() + " " + link.id().substring(0, 7), link.id()));
                    links.add(go);
                }
            }
        }
        links.revalidate();
        links.repaint();
        updateButtons();
        updatePreview();
    }

    // ---- running commands -------------------------------------------------------------------

    /**
     * One command to run: its audit action, and optionally a label and an area
     * to stream its output into.
     */
    private record Step(String action, List<String> command, String label, JTextArea streamTo) {
        Step(String action, List<String> command) {
            this(action, command, null, null);
        }
    }

    /** Runs steps in order off the event thread, auditing each, then hands all outcomes to {@code then}. */
    private void execute(List<Step> steps, Consumer<List<CommandRunner.Outcome>> then) {
        busy = true;
        updateButtons();
        new SwingWorker<List<CommandRunner.Outcome>, String>() {
            private JTextArea target;

            @Override
            protected List<CommandRunner.Outcome> doInBackground() throws IOException, InterruptedException {
                List<CommandRunner.Outcome> outcomes = new ArrayList<>();
                for (Step step : steps) {
                    CommandRunner current = new CommandRunner();
                    runner = current;
                    target = step.streamTo();
                    CommandRunner.Outcome outcome = current.run(step.command(), GitCommands.ENVIRONMENT,
                            line -> {
                                if (target != null) {
                                    publish(line);
                                }
                            });
                    outcomes.add(outcome);
                    audit(step, outcome);
                    if (outcome.cancelled()) {
                        break;
                    }
                }
                return outcomes;
            }

            @Override
            protected void process(List<String> lines) {
                if (target != null) {
                    lines.forEach(line -> target.append(line + "\n"));
                }
            }

            @Override
            protected void done() {
                runner = null;
                busy = false;
                try {
                    then.accept(get());
                } catch (ExecutionException e) {
                    setStatus(BAD, "Could not run git: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                updateButtons();
            }
        }.execute();
    }

    private void audit(Step step, CommandRunner.Outcome outcome) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", display(step.command()));
        if (step.label() != null) {
            details.put("preset", step.label());
        }
        details.put("outcome", outcome.cancelled() ? "cancelled" : outcome.exitCode() == 0 ? "ok" : "failed");
        details.put("exitCode", outcome.exitCode());
        details.put("durationMs", outcome.durationMs());
        details.put("outputLines", outcome.lines().size());
        if (outcome.truncated()) {
            details.put("truncated", true);
        }
        context.audit(step.action(), details);
    }

    void cancel() {
        CommandRunner current = runner;
        if (current != null) {
            current.cancel();
        }
        if (explainPane != null) {
            explainPane.cancel();
        }
    }

    // ---- AI explanation -----------------------------------------------------------------------

    void explainLast() {
        if (explainPane == null || explainPane.isRunning()) {
            return;
        }
        tabs.setSelectedComponent(explainPane);
        if (lastOutput == null) {
            explainPane.showMessage("Run a command or inspect an object first, then ask for an explanation.");
            return;
        }
        try {
            explainPane.explain(lastOutput.command(),
                    GitExplainPrompt.build(lastOutput.command(), lastOutput.exitCode(), lastOutput.lines()));
        } catch (IllegalArgumentException tooLong) {
            explainPane.showMessage(tooLong.getMessage());
        }
    }

    boolean canExplain() {
        return explainPane != null;
    }

    // ---- state ------------------------------------------------------------------------------

    private void updateButtons() {
        boolean explaining = explainPane != null && explainPane.isRunning();
        run.setEnabled(gitAvailable && !busy);
        inspect.setEnabled(gitAvailable && !busy);
        refreshGraph.setEnabled(gitAvailable && !busy);
        stop.setEnabled(busy || explaining);
        explain.setEnabled(lastOutput != null && !busy && !explaining);
    }

    /** The command the current tab would run, shown in "Under the hood". */
    String previewCommand() {
        Path repo = repository != null ? repository
                : Path.of(repositoryField.getText().isBlank() ? "REPOSITORY" : repositoryField.getText().strip());
        if (tabs.getSelectedComponent() == graphTab) {
            commandPane.setCaption("This is the command that draws the graph:");
            return display(GitLog.command(repo, allBranches.isSelected(), (Integer) graphLimit.getValue()));
        }
        if (tabs.getSelectedComponent() == objectsTab) {
            String spec = object.getText().strip();
            commandPane.setCaption("This is the command that shows an object's content (Inspect also runs rev-parse"
                    + " and cat-file -t and -s first):");
            return display(GitCommands.catFile(repo, "-p", spec.isEmpty() ? "OBJECT" : spec));
        }
        commandPane.setCaption("This is the command that runs when you press Run:");
        GitPreset p = (GitPreset) preset.getSelectedItem();
        String v = value.getText().strip();
        try {
            return display(p.command(repo, v, (Integer) limit.getValue()));
        } catch (IllegalArgumentException incomplete) {
            return display(p.command(repo, p.input() == GitPreset.Input.PATH ? "PATH" : "HEAD", (Integer) limit.getValue()));
        }
    }

    private void updatePreview() {
        commandPane.setCommand(previewCommand());
    }

    private void setStatus(Color colour, String text) {
        status.setForeground(colour);
        status.setText(text);
    }

    private void restoreSettings() {
        Preferences p = context.preferences();
        repositoryField.setText(p.get("repository", ""));   // blank: openDefaultRepository asks git
        String presetName = p.get("preset", GitPreset.STATUS.name());
        for (GitPreset g : GitPreset.values()) {
            if (g.name().equals(presetName)) {
                preset.setSelectedItem(g);
            }
        }
        limit.setValue(p.getInt("limit", 50));
        object.setText(p.get("object", "HEAD"));
        allBranches.setSelected(p.getBoolean("graph.allBranches", true));
        graphLimit.setValue(p.getInt("graph.limit", 500));
        allBranches.addActionListener(e -> p.putBoolean("graph.allBranches", allBranches.isSelected()));
        graphLimit.addChangeListener(e -> p.putInt("graph.limit", (Integer) graphLimit.getValue()));
        preset.addActionListener(e -> p.put("preset", ((GitPreset) preset.getSelectedItem()).name()));
        limit.addChangeListener(e -> p.putInt("limit", (Integer) limit.getValue()));
        object.addActionListener(e -> p.put("object", object.getText().strip()));
    }

    void focusFirstField() {
        JComponent target = repository == null ? repositoryField
                : tabs.getSelectedComponent() == objectsTab ? object
                : tabs.getSelectedComponent() == graphTab ? graph : value;
        target.requestFocusInWindow();
    }

    void dispose() {
        cancel();
        commandPane.dispose();
    }

    void copyCommand() {
        commandPane.copyCommand();
    }

    void toggleUnderTheHood() {
        underTheHood.setExpanded(!underTheHood.isExpanded());
    }

    private static String display(List<String> command) {
        return CommandLines.display(GitCommands.ENVIRONMENT, command);
    }

    private static String lastLine(CommandRunner.Outcome outcome) {
        return outcome.lines().isEmpty() ? "no output" : outcome.lines().get(outcome.lines().size() - 1).strip();
    }

    private static String count(int n, String noun) {
        return NumberFormat.getIntegerInstance().format(n) + " " + noun + (n == 1 ? "" : "s");
    }

    private static Font monospace(Font base) {
        return new Font(Font.MONOSPACED, Font.PLAIN, base.getSize());
    }

    /** The Objects tab's tree table. */
    static final class TreeModel extends AbstractTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNS = {"Name", "Type", "Mode", "Object"};
        private transient List<GitObjects.TreeEntry> rows = List.of();

        void setEntries(List<GitObjects.TreeEntry> entries) {
            rows = List.copyOf(entries);
            fireTableDataChanged();
        }

        GitObjects.TreeEntry entry(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            GitObjects.TreeEntry e = rows.get(row);
            return switch (column) {
                case 0 -> e.type().equals("tree") ? e.name() + "/" : e.name();
                case 1 -> e.type();
                case 2 -> e.mode();
                default -> e.id();
            };
        }
    }
}
