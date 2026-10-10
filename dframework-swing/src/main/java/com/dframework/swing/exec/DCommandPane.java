package com.dframework.swing.exec;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.notify.DToast;
import com.dframework.swing.table.DTable;

/**
 * Shows what a tool really runs: the next command, written out in full with a
 * Copy button, the commands run this session (from the audit log), and where
 * the audit log is. Typically placed in a {@code DCollapsiblePane} titled
 * "Under the hood".
 *
 * <pre>{@code
 * DCommandPane commands = new DCommandPane(context.auditLog(), context.plugin().id(), "run");
 * commands.setCommand(CommandLines.display(argv));   // update as the user changes options
 * }</pre>
 *
 * <p>The history lists audit entries from {@code source} whose action is one of
 * {@code actions}, reading their {@code command}, {@code outcome},
 * {@code exitCode} and {@code durationMs} details.
 */
public class DCommandPane extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final transient AuditLog log;
    private final String source;
    private final transient Set<String> actions;
    private final JLabel caption = new JLabel("This is the command that runs:");
    private final JTextField command = new JTextField();
    private final HistoryModel history;
    private final transient Consumer<AuditLog.Entry> listener;

    public DCommandPane(AuditLog log, String source, String... actions) {
        this.log = Objects.requireNonNull(log, "log");
        this.source = Objects.requireNonNull(source, "source");
        this.actions = Set.of(actions);
        this.history = new HistoryModel(actions.length > 1);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 18, 4, 4));

        command.setEditable(false);
        command.setFont(new Font(Font.MONOSPACED, Font.BOLD, command.getFont().getSize()));
        command.getAccessibleContext().setAccessibleName("Command that will run");
        JButton copy = new JButton("Copy");
        copy.setToolTipText("Copy the command, to run it yourself in a terminal");
        copy.addActionListener(e -> copyCommand());
        JPanel commandRow = new JPanel(new BorderLayout(8, 0));
        commandRow.add(new JLabel("$"), BorderLayout.WEST);
        commandRow.add(command, BorderLayout.CENTER);
        commandRow.add(copy, BorderLayout.EAST);

        DTable table = new DTable(history) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText(MouseEvent event) {
                // Commands are long; show the whole one on hover.
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                Object value = row >= 0 && column >= 0 ? getValueAt(row, column) : null;
                return value == null || value.toString().isEmpty() ? null : value.toString();
            }
        };
        table.setEmptyText("Commands you run appear here.");
        // The command is what matters: give it the room, and the short columns just enough.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        for (int c = 0; c < history.getColumnCount() - 1; c++) {
            int width = switch (history.getColumnName(c)) {
                case "Time" -> 70;
                case "What" -> 64;
                case "Result" -> 80;
                default -> 52;
            };
            table.getColumnModel().getColumn(c).setPreferredWidth(width);
            table.getColumnModel().getColumn(c).setMaxWidth(width * 2);
        }
        ToolTipManager.sharedInstance().registerComponent(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(400, 92));   // about four rows; the split lets people drag for more
        String where = log.file().map(Object::toString).orElse("memory only, not saved to a file");
        JLabel auditNote = new JLabel("Every command run is recorded in the audit log: " + where);
        auditNote.setEnabled(false);
        JLabel historyLabel = new JLabel("Run this session:");

        for (JComponent part : List.of(caption, commandRow, historyLabel, scroll, auditNote)) {
            part.setAlignmentX(LEFT_ALIGNMENT);
        }
        add(caption);
        add(Box.createVerticalStrut(4));
        add(commandRow);
        add(Box.createVerticalStrut(10));
        add(historyLabel);
        add(Box.createVerticalStrut(4));
        add(scroll);
        add(Box.createVerticalStrut(6));
        add(auditNote);

        log.recent(source).stream().filter(e -> this.actions.contains(e.action())).forEach(history::add);
        listener = entry -> {
            if (entry.source().equals(source) && this.actions.contains(entry.action())) {
                SwingUtilities.invokeLater(() -> history.add(entry));
            }
        };
        log.addListener(listener);
    }

    /** Shows the command that will run next. */
    public void setCommand(String commandLine) {
        command.setText(commandLine);
        command.setCaretPosition(0);
    }

    public String getCommand() {
        return command.getText();
    }

    /** The sentence above the command, e.g. "This is the command that runs when you press Dig:". */
    public void setCaption(String text) {
        caption.setText(text);
    }

    /** Copies the command to the clipboard and says so. */
    public void copyCommand() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(command.getText()), null);
        DToast.info(this, "Command copied. Paste it into a terminal to run it yourself.");
    }

    /** Commands listed in the history, newest first. */
    public int getHistorySize() {
        return history.getRowCount();
    }

    /** Stops following the audit log. Call when the pane is no longer used. */
    public void dispose() {
        log.removeListener(listener);
    }

    /** Commands run this session, newest first. */
    private static final class HistoryModel extends AbstractTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        private final boolean showAction;
        private final transient List<AuditLog.Entry> rows = new ArrayList<>();

        HistoryModel(boolean showAction) {
            this.showAction = showAction;
        }

        void add(AuditLog.Entry entry) {
            rows.add(0, entry);
            fireTableRowsInserted(0, 0);
        }

        private List<String> columns() {
            return showAction ? List.of("Time", "What", "Result", "ms", "Command") : List.of("Time", "Result", "ms", "Command");
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns().size();
        }

        @Override
        public String getColumnName(int column) {
            return columns().get(column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            AuditLog.Entry e = rows.get(row);
            Map<String, String> d = e.details();
            String name = columns().get(column);
            return switch (name) {
                case "Time" -> CLOCK.format(e.time());
                case "What" -> e.action();
                case "Result" -> d.getOrDefault("outcome", "") + (d.get("exitCode") != null && !"0".equals(d.get("exitCode"))
                        ? " (" + d.get("exitCode") + ")" : "");
                case "ms" -> d.getOrDefault("durationMs", "");
                default -> d.getOrDefault("command", "");
            };
        }
    }
}
