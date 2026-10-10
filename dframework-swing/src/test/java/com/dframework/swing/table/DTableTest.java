package com.dframework.swing.table;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

class DTableTest {

    private final DTable table = new DTable(new Object[][] {
            {1042, "Acme Ltd", "Printer fault", "Open"},
            {1043, "Bolt & Co", "Email down", "Closed"},
            {1044, "Acme Ltd", "New starter laptop", "Closed"},
            {1045, "Cobalt plc", "Printer jam", "On hold"},
    }, new Object[] {"Call", "Customer", "Summary", "Status"});

    @Test
    void sortableOutOfTheBox() {
        assertThat(table.getRowSorter()).isNotNull();
        assertThat(table.getModel().getColumnClass(0)).isEqualTo(Integer.class);
    }

    @Test
    void filtersAcrossColumns() {
        table.setFilterText("acme");
        assertThat(table.getMatchingRowCount()).isEqualTo(2);
        assertThat(table.getTotalRowCount()).isEqualTo(4);

        table.setFilterText("printer -acme");
        assertThat(table.getMatchingRowCount()).isEqualTo(1);
        assertThat(table.getValueAt(0, 1)).isEqualTo("Cobalt plc");

        table.setFilterText("status:closed acme");
        assertThat(table.getMatchingRowCount()).isEqualTo(1);
        assertThat(table.getValueAt(0, 0)).isEqualTo(1044);

        table.setFilterText("");
        assertThat(table.getMatchingRowCount()).isEqualTo(4);
    }

    @Test
    void firesFilterTextChanges() {
        List<Object> seen = new ArrayList<>();
        table.addPropertyChangeListener(DTable.FILTER_TEXT_PROPERTY, e -> seen.add(e.getNewValue()));

        table.setFilterText("  acme  ");

        assertThat(seen).containsExactly("acme");
    }

    @Test
    void filterSurvivesModelReplacement() {
        table.setFilterText("x");
        table.setModel(new DefaultTableModel(new Object[][] {{"x"}, {"y"}, {"xx"}}, new Object[] {"Name"}));

        assertThat(table.getMatchingRowCount()).isEqualTo(2);
    }

    @Test
    void highlightsMatchesSafely() {
        table.setFilterText("co");

        JLabel cell = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);   // "Bolt & Co" (Acme rows filtered)
        assertThat(cell.getText()).startsWith("<html>").contains("Bolt &amp; ").contains(">Co</span>");
    }

    @Test
    void columnTermsOnlyHighlightTheirColumn() {
        table.setFilterText("status:open");

        // Renderers are shared between cells, so read each text straight away.
        String customer = ((JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1)).getText();
        String status = ((JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3)).getText();
        assertThat(customer).doesNotStartWith("<html>");
        assertThat(status).startsWith("<html>");
    }

    @Test
    void stripesAlternateRows() {
        Color even = table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1).getBackground();
        Color odd = table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1).getBackground();
        assertThat(odd).isNotEqualTo(even);

        table.setStriped(false);
        assertThat(table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1).getBackground()).isEqualTo(even);
    }

    @Test
    void stripeDoesNotLeakIntoFollowingRows() {
        // Regression: the renderer kept the stripe colour, so row 2 came out striped too.
        Color row0 = table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1).getBackground();
        table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1);
        Color row2 = table.prepareRenderer(table.getCellRenderer(2, 1), 2, 1).getBackground();

        assertThat(row2).isEqualTo(row0);
    }

    @Test
    void customRendererBackgroundsAreNotStriped() {
        DefaultTableCellRenderer red = new DefaultTableCellRenderer();
        red.setBackground(Color.RED);
        table.getColumnModel().getColumn(3).setCellRenderer(red);

        assertThat(table.prepareRenderer(red, 1, 3).getBackground()).isEqualTo(Color.RED);
        assertThat(table.prepareRenderer(red, 2, 3).getBackground()).isEqualTo(Color.RED);
    }

    @Test
    void packColumnsFitsContent() {
        table.getColumnModel().getColumn(2).setPreferredWidth(5);

        table.packColumns(400);

        assertThat(table.getColumnModel().getColumn(2).getPreferredWidth()).isGreaterThan(50).isLessThanOrEqualTo(400);
    }
}
