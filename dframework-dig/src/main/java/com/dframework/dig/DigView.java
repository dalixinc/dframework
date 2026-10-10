package com.dframework.dig;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.Serial;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.button.DToggleSwitch;
import com.dframework.swing.field.DTextField;
import com.dframework.swing.notify.DToast;
import com.dframework.swing.panel.DCollapsiblePane;
import com.dframework.swing.plugin.PluginContext;
import com.dframework.swing.table.DTable;
import com.dframework.swing.table.DTablePanel;

/** The DNS Dig user interface: the form, the results, and "Under the hood". */
final class DigView extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color GOOD = new Color(0x27AE60);
    private static final Color WARN = new Color(0xD08C00);
    private static final Color BAD = new Color(0xE0443E);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String[][] SERVERS = {
            {"System resolver", ""}, {"Cloudflare  1.1.1.1", "1.1.1.1"}, {"Google  8.8.8.8", "8.8.8.8"},
            {"Quad9  9.9.9.9", "9.9.9.9"}, {"OpenDNS  208.67.222.222", "208.67.222.222"}};

    private final transient PluginContext context;
    private final boolean digAvailable;

    private final DTextField name = new DTextField(26);
    private final JComboBox<String> type = new JComboBox<>(DigOptions.TYPES.toArray(String[]::new));
    private final DTextField server = new DTextField(16);
    private final JSpinner timeout = new JSpinner(new SpinnerNumberModel(DigOptions.DEFAULT_TIMEOUT, 1, 30, 1));
    private final transient Map<DigOptions.Flag, DToggleSwitch> switches = new EnumMap<>(DigOptions.Flag.class);
    private final JButton run = new JButton("Dig");
    private final JButton stop = new JButton("Stop");

    private final JLabel status = new JLabel(" ");
    private final RecordModel records = new RecordModel();
    private final DTable answers = new DTable(records);
    private final JTextArea raw = new JTextArea();
    private final JTabbedPane results = new JTabbedPane();

    private final JTextField commandLine = new JTextField();
    private final HistoryModel history = new HistoryModel();
    private final DCollapsiblePane underTheHood;

    private transient DigRunner runner;
    private transient Consumer<AuditLog.Entry> historyListener;

    DigView(PluginContext context) {
        super(new BorderLayout(0, 8));
        this.context = context;
        this.digAvailable = DigRunner.locate(DigCommand.EXECUTABLE).isPresent();
        setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        if (!digAvailable) {
            top.add(missingDigBanner(), BorderLayout.NORTH);
        }
        top.add(form(), BorderLayout.CENTER);
        top.add(status, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        raw.setEditable(false);
        raw.setFont(monospace(raw.getFont()));
        answers.setEmptyText("No records yet. Enter a name and press Dig.");
        answers.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);   // the Data column takes what's left
        results.addTab("Answers", new DTablePanel(answers, "Filter answers"));
        results.addTab("Raw output", new JScrollPane(raw));
        add(results, BorderLayout.CENTER);

        underTheHood = new DCollapsiblePane("Under the hood", underTheHoodContent());
        underTheHood.rememberState(context.preferences(), "underTheHood.expanded");
        add(underTheHood, BorderLayout.SOUTH);

        restoreSettings();
        wireUp();
        updatePreview();
    }

    // ---- building the form -------------------------------------------------------------

    private JComponent form() {
        name.setInputFilter(DigInput.nameFilter());
        name.setValidator(DigInput.nameValidator(this::isReverse));
        name.putClientProperty("JTextField.placeholderText", "example.com");
        server.setInputFilter(DigInput.serverFilter());
        server.setValidator(DigInput.serverValidator());
        server.putClientProperty("JTextField.placeholderText", "system resolver");
        stop.setEnabled(false);
        run.setEnabled(digAvailable);
        run.setToolTipText("Run dig (Enter)");

        JButton presets = new JButton("▾");
        presets.setToolTipText("Choose a public DNS server");
        presets.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (String[] preset : SERVERS) {
                JMenuItem item = new JMenuItem(preset[0]);
                item.addActionListener(ev -> server.setText(preset[1]));
                menu.add(item);
            }
            menu.show(presets, 0, presets.getHeight());
        });

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        grid.add(new JLabel("Name"), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        grid.add(name, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        grid.add(new JLabel("Type"), c);
        grid.add(type, c);
        grid.add(run, c);
        grid.add(stop, c);

        c.gridy = 1;
        grid.add(new JLabel("Server"), c);
        JPanel serverRow = new JPanel(new BorderLayout(4, 0));
        serverRow.add(server, BorderLayout.CENTER);
        serverRow.add(presets, BorderLayout.EAST);
        c.fill = GridBagConstraints.HORIZONTAL;
        grid.add(serverRow, c);
        c.fill = GridBagConstraints.NONE;
        grid.add(new JLabel("Timeout"), c);
        JPanel timeoutRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        timeoutRow.add(timeout);
        timeoutRow.add(new JLabel("s"));
        c.gridwidth = 3;
        grid.add(timeoutRow, c);
        c.gridwidth = 1;

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 14, 2));
        for (DigOptions.Flag flag : DigOptions.Flag.values()) {
            DToggleSwitch toggle = new DToggleSwitch(flag.label());
            toggle.setToolTipText(flag.description());
            switches.put(flag, toggle);
            options.add(toggle);
        }
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        grid.add(options, c);
        return grid;
    }

    private JComponent missingDigBanner() {
        JLabel banner = new JLabel("<html><b>dig was not found on this computer's PATH.</b> Install it, then reopen this tool:"
                + " <code>sudo apt install bind9-dnsutils</code> (Debian, Ubuntu), <code>sudo dnf install bind-utils</code>"
                + " (Fedora), or <code>brew install bind</code> (macOS). You can still build commands below.</html>");
        banner.setForeground(BAD);
        banner.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return banner;
    }

    private JComponent underTheHoodContent() {
        commandLine.setEditable(false);
        commandLine.setFont(monospace(commandLine.getFont()).deriveFont(Font.BOLD));
        commandLine.getAccessibleContext().setAccessibleName("Command that will run");
        JButton copy = new JButton("Copy");
        copy.setToolTipText("Copy the command, to run it yourself in a terminal");
        copy.addActionListener(e -> copyCommand());
        JPanel commandRow = new JPanel(new BorderLayout(8, 0));
        commandRow.add(new JLabel("$"), BorderLayout.WEST);
        commandRow.add(commandLine, BorderLayout.CENTER);
        commandRow.add(copy, BorderLayout.EAST);

        DTable historyTable = new DTable(history);
        historyTable.setEmptyText("Commands you run appear here.");
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(520);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setPreferredSize(new Dimension(400, 110));

        String where = context.auditLog().file().map(Object::toString).orElse("memory only, not saved to a file");
        JLabel auditNote = new JLabel("Every run is recorded in the audit log: " + where);
        auditNote.setEnabled(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel nextLabel = new JLabel("This is the command that runs when you press Dig:");
        JLabel historyLabel = new JLabel("Run this session:");
        for (JComponent part : List.of(nextLabel, commandRow, historyLabel, historyScroll, auditNote)) {
            part.setAlignmentX(LEFT_ALIGNMENT);
        }
        content.add(nextLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(commandRow);
        content.add(Box.createVerticalStrut(10));
        content.add(historyLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(historyScroll);
        content.add(Box.createVerticalStrut(6));
        content.add(auditNote);
        content.setBorder(BorderFactory.createEmptyBorder(4, 18, 4, 4));

        for (AuditLog.Entry entry : context.auditLog().recent(context.plugin().id())) {
            if (entry.action().equals("run")) {
                history.add(entry);
            }
        }
        historyListener = entry -> {
            if (entry.source().equals(context.plugin().id()) && entry.action().equals("run")) {
                SwingUtilities.invokeLater(() -> history.add(entry));
            }
        };
        context.auditLog().addListener(historyListener);
        return content;
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
        name.getDocument().addDocumentListener(preview);
        server.getDocument().addDocumentListener(preview);
        type.addActionListener(e -> updatePreview());
        timeout.addChangeListener(e -> updatePreview());
        switches.forEach((flag, toggle) -> toggle.addItemListener(e -> {
            if (flag == DigOptions.Flag.REVERSE) {
                type.setEnabled(!toggle.isSelected());
                name.setValidator(DigInput.nameValidator(this::isReverse));   // re-check with the new rule
                name.putClientProperty("JTextField.placeholderText", toggle.isSelected() ? "8.8.8.8" : "example.com");
            }
            if (flag == DigOptions.Flag.SHORT) {
                switches.get(DigOptions.Flag.ANSWER_ONLY).setEnabled(!toggle.isSelected());
            }
            updatePreview();
        }));
        name.addActionListener(e -> run());
        server.addActionListener(e -> run());
        run.addActionListener(e -> run());
        stop.addActionListener(e -> cancel());
    }

    // ---- behaviour ------------------------------------------------------------------------

    private boolean isReverse() {
        DToggleSwitch reverse = switches.get(DigOptions.Flag.REVERSE);
        return reverse != null && reverse.isSelected();
    }

    DigOptions currentOptions() {
        Set<DigOptions.Flag> flags = EnumSet.noneOf(DigOptions.Flag.class);
        switches.forEach((flag, toggle) -> {
            if (toggle.isSelected()) {
                flags.add(flag);
            }
        });
        return new DigOptions(name.getText(), (String) type.getSelectedItem(), server.getText(), flags,
                (Integer) timeout.getValue());
    }

    /** The command as it stands, for the preview. "NAME" stands in until a name is typed. */
    String previewCommand() {
        DigOptions options = currentOptions();
        if (options.name().isEmpty()) {
            options = new DigOptions("NAME", options.type(), options.server(), options.flags(), options.timeoutSeconds());
        }
        return DigCommand.display(DigCommand.arguments(options));
    }

    private void updatePreview() {
        commandLine.setText(previewCommand());
        commandLine.setCaretPosition(0);
    }

    /** Runs dig with the form's current options. */
    void run() {
        if (runner != null || !digAvailable) {
            return;
        }
        boolean nameOk = name.validateInput();
        boolean serverOk = server.validateInput();
        if (!nameOk || !serverOk) {
            (nameOk ? server : name).requestFocusInWindow();
            return;
        }
        DigOptions options = currentOptions();
        List<String> command = DigCommand.arguments(options);
        String display = DigCommand.display(command);
        saveSettings(options);

        raw.setText("$ " + display + "\n\n");
        records.setRecords(List.of());
        status.setForeground(name.getForeground());
        status.setText("Running…  " + display);
        run.setEnabled(false);
        stop.setEnabled(true);

        DigRunner current = new DigRunner();
        runner = current;
        new SwingWorker<DigRunner.Outcome, String>() {
            @Override
            protected DigRunner.Outcome doInBackground() throws Exception {
                return current.run(command, this::publish);
            }

            @Override
            protected void process(List<String> lines) {
                lines.forEach(line -> raw.append(line + "\n"));
            }

            @Override
            protected void done() {
                runner = null;
                run.setEnabled(true);
                stop.setEnabled(false);
                try {
                    finished(options, display, get());
                } catch (ExecutionException e) {
                    failed(options, display, e.getCause());
                } catch (InterruptedException | CancellationException e) {
                    failed(options, display, e);
                }
            }
        }.execute();
    }

    void cancel() {
        DigRunner current = runner;
        if (current != null) {
            current.cancel();
        }
    }

    /** Stops any running lookup and detaches from the audit log. */
    void dispose() {
        cancel();
        context.auditLog().removeListener(historyListener);
    }

    void copyCommand() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(previewCommand()), null);
        DToast.info(this, "Command copied. Paste it into a terminal to run it yourself.");
    }

    void toggleUnderTheHood() {
        underTheHood.setExpanded(!underTheHood.isExpanded());
    }

    void focusName() {
        name.requestFocusInWindow();
    }

    private void finished(DigOptions options, String display, DigRunner.Outcome outcome) {
        DigOutput.Result result = DigOutput.parse(outcome.lines(), options);
        records.setRecords(result.records());
        answers.packColumns(420);   // size columns to this result, so long values like MX hosts fit
        DigOutput.Summary s = result.summary();
        String outcomeText;
        if (outcome.cancelled()) {
            outcomeText = "cancelled";
            setStatus(WARN, "Cancelled after " + outcome.durationMs() + " ms.");
        } else if (outcome.exitCode() != 0) {
            outcomeText = "failed";
            String why = s.problems().isEmpty() ? "see Raw output" : s.problems().get(s.problems().size() - 1);
            setStatus(BAD, "dig failed (exit code " + outcome.exitCode() + "): " + why);
            DToast.error(this, "dig failed: " + why);
        } else {
            outcomeText = "ok";
            setStatus(colourFor(s.status()), summaryText(s, outcome));
        }
        results.setSelectedIndex(result.records().isEmpty() ? 1 : 0);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", display);
        details.put("name", options.name());
        details.put("type", options.has(DigOptions.Flag.REVERSE) ? "PTR" : options.type());
        details.put("server", options.server().isEmpty() ? "system resolver" : options.server());
        details.put("outcome", outcomeText);
        details.put("exitCode", outcome.exitCode());
        details.put("durationMs", outcome.durationMs());
        details.put("status", s.status());
        details.put("records", result.records().size());
        details.put("outputLines", outcome.lines().size());
        context.audit("run", details);
    }

    private void failed(DigOptions options, String display, Throwable error) {
        String message = error instanceof IOException ? error.getMessage() : String.valueOf(error);
        setStatus(BAD, "Could not run dig: " + message);
        raw.append("\n" + message + "\n");
        DToast.error(this, "Could not run dig: " + message);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", display);
        details.put("name", options.name());
        details.put("outcome", "error");
        details.put("error", message);
        context.audit("run", details);
    }

    private static String summaryText(DigOutput.Summary s, DigRunner.Outcome outcome) {
        List<String> parts = new ArrayList<>();
        if (s.status() != null) {
            parts.add(s.status());
        }
        if (s.hops() > 0) {
            parts.add("traced through " + s.hops() + " server" + (s.hops() == 1 ? "" : "s"));
        }
        parts.add(s.answers() + (s.answers() == 1 ? " answer" : " answers"));
        parts.add((s.queryTimeMs() != null ? s.queryTimeMs() : outcome.durationMs()) + " ms");
        if (s.server() != null) {
            parts.add("asked " + s.server() + (s.protocol() != null ? " over " + s.protocol() : ""));
        }
        if (s.messageSize() != null) {
            parts.add(s.messageSize() + " bytes");
        }
        if (!s.problems().isEmpty()) {
            parts.add(s.problems().size() + (s.problems().size() == 1 ? " warning" : " warnings") + " (see Raw output)");
        }
        return String.join("  ·  ", parts);
    }

    private static Color colourFor(String dnsStatus) {
        if (dnsStatus == null || dnsStatus.equals("NOERROR")) {
            return GOOD;
        }
        return dnsStatus.equals("NXDOMAIN") ? WARN : BAD;
    }

    private void setStatus(Color colour, String text) {
        status.setForeground(colour);
        status.setText(text);
    }

    // ---- remembered settings ---------------------------------------------------------------

    private void restoreSettings() {
        Preferences p = context.preferences();
        name.setText(p.get("name", ""));
        type.setSelectedItem(p.get("type", "A"));
        server.setText(p.get("server", ""));
        timeout.setValue(p.getInt("timeout", DigOptions.DEFAULT_TIMEOUT));
        switches.forEach((flag, toggle) -> toggle.setSelected(p.getBoolean("flag." + flag.name(), false)));
        type.setEnabled(!isReverse());
        switches.get(DigOptions.Flag.ANSWER_ONLY).setEnabled(!switches.get(DigOptions.Flag.SHORT).isSelected());
    }

    private void saveSettings(DigOptions options) {
        Preferences p = context.preferences();
        p.put("name", options.name());
        p.put("type", options.type());
        p.put("server", options.server());
        p.putInt("timeout", options.timeoutSeconds());
        for (DigOptions.Flag flag : DigOptions.Flag.values()) {
            p.putBoolean("flag." + flag.name(), options.has(flag));
        }
    }

    private static Font monospace(Font base) {
        return new Font(Font.MONOSPACED, Font.PLAIN, base.getSize());
    }

    // ---- table models ---------------------------------------------------------------------------

    /** The Answers table. */
    static final class RecordModel extends AbstractTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNS = {"Section", "Name", "TTL", "Class", "Type", "Data"};
        private transient List<DigOutput.DnsRecord> rows = List.of();

        void setRecords(List<DigOutput.DnsRecord> records) {
            rows = List.copyOf(records);
            fireTableDataChanged();
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
        public Class<?> getColumnClass(int column) {
            return column == 2 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            DigOutput.DnsRecord r = rows.get(row);
            return switch (column) {
                case 0 -> r.section();
                case 1 -> r.name();
                case 2 -> r.ttl();
                case 3 -> r.dnsClass();
                case 4 -> r.type();
                default -> r.data();
            };
        }
    }

    /** Commands run this session, newest first, from the audit log. */
    static final class HistoryModel extends AbstractTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNS = {"Time", "Result", "ms", "Command"};
        private final transient List<AuditLog.Entry> rows = new ArrayList<>();

        void add(AuditLog.Entry entry) {
            rows.add(0, entry);
            fireTableRowsInserted(0, 0);
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
            AuditLog.Entry e = rows.get(row);
            Map<String, String> d = e.details();
            return switch (column) {
                case 0 -> CLOCK.format(e.time());
                case 1 -> d.getOrDefault("outcome", "") + (d.get("exitCode") != null && !"0".equals(d.get("exitCode"))
                        ? " (" + d.get("exitCode") + ")" : "");
                case 2 -> d.getOrDefault("durationMs", "");
                default -> d.getOrDefault("command", "");
            };
        }
    }
}
