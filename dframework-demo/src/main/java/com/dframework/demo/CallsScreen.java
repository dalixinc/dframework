package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.dframework.swing.busy.DBusyPanel;
import com.dframework.swing.nav.DBreadcrumbBar;
import com.dframework.swing.nav.DBreadcrumbBar.Crumb;
import com.dframework.swing.notify.DToast;
import com.dframework.swing.table.DTable;
import com.dframework.swing.table.DTablePanel;

/**
 * The searchable call log, with a busy overlay on Refresh and drill-down to a
 * call's details that pushes a breadcrumb.
 */
final class CallsScreen {

    private static final int CALLS = 300;

    private CallsScreen() {
    }

    /** @param filter pre-filled search, e.g. "engineer:dale -closed" */
    static JComponent create(String filter, DBreadcrumbBar crumbs) {
        CallLog log = new CallLog(CALLS);
        DTable table = new DTable(log);
        table.packColumns(320);
        DTablePanel tablePanel = new DTablePanel(table);
        table.setFilterText(filter);

        CardLayout cards = new CardLayout();
        JPanel screen = new JPanel(cards);
        JPanel detailHolder = new JPanel(new BorderLayout());

        DBusyPanel busy = new DBusyPanel(tablePanel);
        JButton refresh = new JButton("Refresh");
        refresh.setToolTipText("Pretend to reload from the Tracker 2 database");
        refresh.addActionListener(e -> busy.run("Loading calls from tracker2sqlb…", true, progress -> {
            for (int step = 0; step <= 20; step++) {
                if (progress.isCancelled()) {
                    return null;
                }
                progress.setProgress(step * 5);
                Thread.sleep(80);
            }
            return CALLS;
        }, count -> DToast.success(refresh, "Refreshed " + count + " calls."), error -> DToast.error(refresh,
                "Refresh failed: " + error.getMessage())));
        JLabel hint = new JLabel("Double-click a call to open it");
        hint.setEnabled(false);
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        toolbar.add(refresh, BorderLayout.WEST);
        toolbar.add(hint, BorderLayout.CENTER);

        JPanel list = new JPanel(new BorderLayout());
        list.add(toolbar, BorderLayout.NORTH);
        list.add(busy, BorderLayout.CENTER);
        screen.add(list, "list");
        screen.add(detailHolder, "detail");

        Crumb[] open = new Crumb[1];
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int view = table.rowAtPoint(e.getPoint());
                if (e.getClickCount() != 2 || view < 0) {
                    return;
                }
                int row = table.convertRowIndexToModel(view);
                detailHolder.removeAll();
                detailHolder.add(detail(log, row, crumbs), BorderLayout.CENTER);
                cards.show(screen, "detail");
                open[0] = Crumb.of("Call #" + log.getValueAt(row, 0));
                crumbs.push(open[0]);
            }
        });
        // Back to the list whenever the call's crumb leaves the trail (clicked away, or another screen chosen).
        crumbs.addPropertyChangeListener(DBreadcrumbBar.PATH_PROPERTY, e -> {
            if (open[0] != null && !crumbs.getPath().contains(open[0])) {
                open[0] = null;
                cards.show(screen, "list");
            }
        });
        return screen;
    }

    private static JComponent detail(CallLog log, int row, DBreadcrumbBar crumbs) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Call #" + log.getValueAt(row, 0)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        for (int col = 1; col < log.getColumnCount(); col++) {
            c.gridy = col;
            c.gridx = 0;
            c.weightx = 0;
            JLabel name = new JLabel(log.getColumnName(col));
            name.setEnabled(false);
            form.add(name, c);
            c.gridx = 1;
            c.weightx = 1;
            JLabel value = new JLabel(String.valueOf(log.getValueAt(row, col)));
            value.setFont(value.getFont().deriveFont(Font.BOLD));
            form.add(value, c);
        }
        JButton back = new JButton("← Back to calls");
        back.addActionListener(e -> crumbs.pop());
        c.gridy = log.getColumnCount();
        c.gridx = 1;
        c.weightx = 1;
        form.add(back, c);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }
}
