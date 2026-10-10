package com.dframework.swing.table;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

class DTablePanelTest {

    private final DefaultTableModel model = new DefaultTableModel(new Object[][] {
            {"Acme", "Open"}, {"Bolt", "Closed"}, {"Acme", "Closed"}}, new Object[] {"Customer", "Status"});
    private final DTablePanel panel = new DTablePanel(new DTable(model));

    @Test
    void searchBoxFiltersTheTableAndCountFollows() {
        panel.getSearchField().setSearchDelay(0);
        assertThat(panel.getCountText()).isEqualTo("3 rows");

        panel.getSearchField().setText("acme");

        assertThat(panel.getTable().getMatchingRowCount()).isEqualTo(2);
        assertThat(panel.getCountText()).isEqualTo("2 of 3 rows");
    }

    @Test
    void countFollowsModelChanges() {
        model.addRow(new Object[] {"Cobalt", "Open"});

        assertThat(panel.getCountText()).isEqualTo("4 rows");
    }

    @Test
    void settingTheFilterInCodeUpdatesTheSearchBox() {
        panel.getTable().setFilterText("status:open");

        assertThat(panel.getSearchField().getText()).isEqualTo("status:open");
        assertThat(panel.getCountText()).isEqualTo("1 of 3 rows");
    }
}
