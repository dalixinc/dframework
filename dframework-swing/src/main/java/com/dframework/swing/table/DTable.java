package com.dframework.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.DefaultRowSorter;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * A {@link JTable} that is ready to use: sortable by clicking headers, striped
 * rows, a message when empty, and live filtering with matches highlighted.
 *
 * <p>{@link #setFilterText(String)} understands a small syntax:
 * <ul>
 * <li>{@code acme printer}: rows containing both words, in any columns
 * <li>{@code "printer fault"}: a phrase
 * <li>{@code -closed}: rows <em>not</em> containing the word
 * <li>{@code status:open}: the word in a column whose name starts with "status"
 * </ul>
 *
 * <p>Pair it with a search box using {@link DTablePanel}.
 */
public class DTable extends JTable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Bound property fired when the filter text changes. */
    public static final String FILTER_TEXT_PROPERTY = "filterText";

    private static final int CELL_PADDING = 12;
    private static final int PACK_SAMPLE_ROWS = 500;

    private String filterText = "";
    private transient List<TableFilter.Term> terms = List.of();
    private boolean striped = true;
    private boolean highlightMatches = true;
    private String emptyText = "No rows";
    private String noMatchesText = "No matching rows";
    /** A renderer whose background we striped, to reset before it renders the next cell. */
    private transient DefaultTableCellRenderer stripedRenderer;

    public DTable() {
        this(new DefaultTableModel());
    }

    public DTable(Object[][] rows, Object[] columnNames) {
        this(new DefaultTableModel(rows, columnNames) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int column) {
                // Use the first non-null value's type, so numbers sort numerically.
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return Object.class;
            }
        });
    }

    public DTable(TableModel model) {
        super(model);
        setAutoCreateRowSorter(true);
        setFillsViewportHeight(true);
        applyFilter();
    }

    @Override
    public void setModel(TableModel model) {
        super.setModel(model);
        // A new model brings a new row sorter, which has lost the filter. Not yet set
        // up while JTable's own constructor is running.
        if (filterText != null) {
            applyFilter();
        }
    }

    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        super.setRowSorter(sorter);
        if (filterText != null) {
            applyFilter();
        }
    }

    /** The current filter text; empty when unfiltered. */
    public String getFilterText() {
        return filterText;
    }

    /** Shows only rows matching {@code text} (see the class description for the syntax). */
    public void setFilterText(String text) {
        String old = filterText;
        filterText = text == null ? "" : text.strip();
        applyFilter();
        firePropertyChange(FILTER_TEXT_PROPERTY, old, filterText);
    }

    /** Rows passing the filter. */
    public int getMatchingRowCount() {
        return getRowCount();
    }

    /** All rows in the model, filtered or not. */
    public int getTotalRowCount() {
        return getModel().getRowCount();
    }

    public boolean isStriped() {
        return striped;
    }

    /** Alternate row shading. Default on. */
    public void setStriped(boolean striped) {
        this.striped = striped;
        repaint();
    }

    public boolean isHighlightMatches() {
        return highlightMatches;
    }

    /** Whether text matching the filter is highlighted in cells. Default on. */
    public void setHighlightMatches(boolean highlightMatches) {
        this.highlightMatches = highlightMatches;
        repaint();
    }

    /** Message shown when the model has no rows. */
    public void setEmptyText(String emptyText) {
        this.emptyText = Objects.requireNonNull(emptyText, "emptyText");
        repaint();
    }

    /** Message shown when rows exist but none match the filter. */
    public void setNoMatchesText(String noMatchesText) {
        this.noMatchesText = Objects.requireNonNull(noMatchesText, "noMatchesText");
        repaint();
    }

    /**
     * Sizes each column to fit its header and contents (sampling the first few
     * hundred rows), capped at {@code maxWidth} pixels.
     */
    public void packColumns(int maxWidth) {
        for (int view = 0; view < getColumnCount(); view++) {
            TableColumn column = getColumnModel().getColumn(view);
            TableCellRenderer headerRenderer = column.getHeaderRenderer() != null
                    ? column.getHeaderRenderer()
                    : getTableHeader() != null ? getTableHeader().getDefaultRenderer() : null;
            int width = 0;
            if (headerRenderer != null) {
                width = headerRenderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, view)
                        .getPreferredSize().width;
            }
            int rows = Math.min(getRowCount(), PACK_SAMPLE_ROWS);
            for (int row = 0; row < rows; row++) {
                Component c = super.prepareRenderer(getCellRenderer(row, view), row, view);
                width = Math.max(width, c.getPreferredSize().width);
            }
            column.setPreferredWidth(Math.min(maxWidth, width + CELL_PADDING));
        }
    }

    private void applyFilter() {
        TableModel model = getModel();
        List<String> names = new ArrayList<>();
        for (int c = 0; c < model.getColumnCount(); c++) {
            names.add(model.getColumnName(c));
        }
        terms = TableFilter.parse(filterText, names);
        if (getRowSorter() instanceof DefaultRowSorter<?, ?> sorter) {
            @SuppressWarnings("unchecked")
            DefaultRowSorter<TableModel, Integer> typed = (DefaultRowSorter<TableModel, Integer>) sorter;
            List<TableFilter.Term> current = terms;
            typed.setRowFilter(current.isEmpty() ? null : new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    return TableFilter.matches(current, entry::getStringValue, entry.getValueCount());
                }
            });
        }
        repaint();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        // DefaultTableCellRenderer.setBackground sticks for every later cell, not just
        // this one, so undo the previous stripe before rendering.
        if (stripedRenderer != null) {
            stripedRenderer.setBackground(null);
            stripedRenderer = null;
        }
        Component c = super.prepareRenderer(renderer, row, column);
        boolean selected = isCellSelected(row, column);
        // Only stripe cells using the table's own background, leaving custom-coloured
        // renderers (and themes that stripe tables themselves) alone.
        if (striped && !selected && row % 2 == 1 && Objects.equals(c.getBackground(), getBackground())) {
            c.setBackground(stripe(getBackground(), getForeground()));
            if (c instanceof DefaultTableCellRenderer d) {
                stripedRenderer = d;
            }
        }
        if (highlightMatches && !terms.isEmpty() && c instanceof JLabel label) {
            String text = label.getText();
            if (text != null && !text.isEmpty() && !text.regionMatches(true, 0, "<html>", 0, 6)) {
                String html = highlight(text, convertColumnIndexToModel(column), selected);
                if (html != null) {
                    label.setText(html);
                }
            }
        }
        return c;
    }

    /** HTML for {@code text} with filter matches marked, or {@code null} if nothing matches. */
    private String highlight(String text, int modelColumn, boolean selected) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean[] marked = new boolean[text.length()];
        boolean any = false;
        for (TableFilter.Term term : terms) {
            if (term.negated() || term.column() >= 0 && term.column() != modelColumn) {
                continue;
            }
            for (int at = lower.indexOf(term.text()); at >= 0; at = lower.indexOf(term.text(), at + 1)) {
                for (int i = at; i < at + term.text().length(); i++) {
                    marked[i] = true;
                }
                any = true;
            }
        }
        if (!any) {
            return null;
        }
        String open = selected
                ? "<b><u>"
                : "<span style='background-color:" + hex(highlightColor()) + "'>";
        String close = selected ? "</u></b>" : "</span>";
        StringBuilder html = new StringBuilder("<html><nobr>");
        for (int i = 0; i < text.length(); i++) {
            if (marked[i] && (i == 0 || !marked[i - 1])) {
                html.append(open);
            }
            char ch = text.charAt(i);
            html.append(switch (ch) {
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '&' -> "&amp;";
                default -> String.valueOf(ch);
            });
            if (marked[i] && (i == text.length() - 1 || !marked[i + 1])) {
                html.append(close);
            }
        }
        return html.append("</nobr></html>").toString();
    }

    private Color highlightColor() {
        return isDark(getBackground()) ? new Color(0x806A00) : new Color(0xFFE873);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getRowCount() > 0) {
            return;
        }
        String message = getTotalRowCount() == 0 ? emptyText : noMatchesText;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color disabled = UIManager.getColor("Label.disabledForeground");
            g2.setColor(disabled != null ? disabled : Color.GRAY);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int width = getParent() != null ? getParent().getWidth() : getWidth();
            g2.drawString(message, (width - fm.stringWidth(message)) / 2, fm.getHeight() * 2);
        } finally {
            g2.dispose();
        }
    }

    private static Color stripe(Color background, Color foreground) {
        float amount = isDark(background) ? 0.06f : 0.035f;
        return new Color(
                Math.round(background.getRed() + (foreground.getRed() - background.getRed()) * amount),
                Math.round(background.getGreen() + (foreground.getGreen() - background.getGreen()) * amount),
                Math.round(background.getBlue() + (foreground.getBlue() - background.getBlue()) * amount));
    }

    private static boolean isDark(Color c) {
        return (c.getRed() + c.getGreen() + c.getBlue()) / 3 < 128;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
