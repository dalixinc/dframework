package com.dframework.swing.table;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.dframework.swing.field.DSearchField;

/**
 * A {@link DTable} with a search box and a row count above it.
 *
 * <pre>{@code
 * DTablePanel calls = new DTablePanel(new DTable(callModel));
 * calls.getSearchField().setText("status:open");
 * }</pre>
 *
 * <p><kbd>Ctrl+F</kbd> anywhere in the panel jumps to the search box.
 */
public class DTablePanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final DTable table;
    private final DSearchField searchField;
    private final JLabel countLabel = new JLabel();
    private final transient RowSorterListener sorterListener = e -> updateCount();
    private final transient TableModelListener modelListener = e -> updateCount();

    public DTablePanel(DTable table) {
        this(table, "Search  (try  word  \"a phrase\"  -exclude  column:word)");
    }

    /** @param placeholder hint shown in the empty search box */
    public DTablePanel(DTable table, String placeholder) {
        super(new BorderLayout(0, 6));
        this.table = table;
        this.searchField = new DSearchField(placeholder);
        searchField.onSearch(table::setFilterText);
        countLabel.setEnabled(false);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.add(searchField, BorderLayout.CENTER);
        top.add(countLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        attach(table.getRowSorter());
        table.getModel().addTableModelListener(modelListener);
        table.addPropertyChangeListener("rowSorter", e -> {
            if (e.getOldValue() instanceof RowSorter<?> old) {
                old.removeRowSorterListener(sorterListener);
            }
            attach((RowSorter<?>) e.getNewValue());
        });
        table.addPropertyChangeListener("model", e -> {
            ((TableModel) e.getOldValue()).removeTableModelListener(modelListener);
            ((TableModel) e.getNewValue()).addTableModelListener(modelListener);
            updateCount();
        });
        table.addPropertyChangeListener(DTable.FILTER_TEXT_PROPERTY, e -> {
            // Keep the box in step when the filter is set from code.
            if (!searchField.getText().strip().equals(table.getFilterText())) {
                searchField.setText(table.getFilterText());
            }
            updateCount();
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "dframework.table.find");
        getActionMap().put("dframework.table.find", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        updateCount();
    }

    public DTable getTable() {
        return table;
    }

    public DSearchField getSearchField() {
        return searchField;
    }

    /** The "12 of 300 rows" text currently shown. */
    public String getCountText() {
        return countLabel.getText();
    }

    private void attach(RowSorter<?> sorter) {
        if (sorter != null) {
            sorter.addRowSorterListener(sorterListener);
        }
        updateCount();
    }

    private void updateCount() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        int total = table.getTotalRowCount();
        int shown = table.getMatchingRowCount();
        String rows = total == 1 ? " row" : " rows";
        countLabel.setText(table.getFilterText().isEmpty() || shown == total
                ? format.format(total) + rows
                : format.format(shown) + " of " + format.format(total) + rows);
    }
}
