package com.dframework.swing.nav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.Test;

class DOutlookBarTest {

    private final DOutlookBar bar = new DOutlookBar();
    private final List<String> selected = new ArrayList<>();
    private final DOutlookBar.Group logging = bar.addGroup("Logging");
    private final DOutlookBar.Group reports = bar.addGroup("Reports");
    private final DOutlookBar.Group admin = bar.addGroup("Admin");
    private final DOutlookBar.Item logCall = logging.addItem("Log Call", null, e -> selected.add("log"));
    private final DOutlookBar.Item customers = reports.addItem("Customer\nGroups", null, e -> selected.add("cust"));

    @Test
    void firstGroupOpensWithOthersAtTheBottom() {
        assertThat(bar.getSelectedGroup()).contains(logging);
        assertThat(headers(BorderLayout.NORTH)).containsExactly("Logging");
        assertThat(headers(BorderLayout.SOUTH)).containsExactly("Reports", "Admin");
    }

    @Test
    void openingAGroupMovesLaterHeadersDown() {
        bar.setSelectedGroup(reports);

        assertThat(headers(BorderLayout.NORTH)).containsExactly("Logging", "Reports");
        assertThat(headers(BorderLayout.SOUTH)).containsExactly("Admin");
    }

    @Test
    void clickingAHeaderOpensItsGroup() {
        header(BorderLayout.SOUTH, "Admin").doClick();

        assertThat(bar.getSelectedGroup()).contains(admin);
        assertThat(headers(BorderLayout.SOUTH)).isEmpty();
    }

    @Test
    void selectingAnItemOpensItsGroupAndRunsItsAction() {
        List<Object> changes = new ArrayList<>();
        bar.addPropertyChangeListener(DOutlookBar.SELECTED_ITEM_PROPERTY, e -> changes.add(e.getNewValue()));

        bar.setSelectedItem(customers);

        assertThat(bar.getSelectedGroup()).contains(reports);
        assertThat(bar.getSelectedItem()).contains(customers);
        assertThat(selected).containsExactly("cust");
        assertThat(changes).containsExactly(customers);
    }

    @Test
    void itemsAreExposedPerGroup() {
        assertThat(logging.getItems()).containsExactly(logCall);
        assertThat(customers.getText()).isEqualTo("Customer\nGroups");
        assertThat(customers.group()).isSameAs(reports);
    }

    @Test
    void rejectsGroupsFromAnotherBar() {
        DOutlookBar.Group foreign = new DOutlookBar().addGroup("Elsewhere");

        assertThatThrownBy(() -> bar.setSelectedGroup(foreign)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void classicStyleAndBack() {
        bar.applyClassicStyle();
        assertThat(bar.getContentBackground()).isEqualTo(new Color(0xACA899));
        assertThat(bar.getItemForeground()).isEqualTo(Color.WHITE);

        bar.applyThemeStyle();
        assertThat(bar.getContentBackground()).isNull();
    }

    private List<String> headers(String position) {
        Container stack = (Container) ((BorderLayout) bar.getLayout()).getLayoutComponent(position);
        return Arrays.stream(stack.getComponents()).map(c -> ((AbstractButton) c).getText()).toList();
    }

    private AbstractButton header(String position, String title) {
        Container stack = (Container) ((BorderLayout) bar.getLayout()).getLayoutComponent(position);
        for (Component c : stack.getComponents()) {
            if (((AbstractButton) c).getText().equals(title)) {
                return (AbstractButton) c;
            }
        }
        throw new AssertionError(title);
    }
}
