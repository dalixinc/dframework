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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatClientProperties;

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
    void overflowingGroupScrollsWithoutSqueezingItems() {
        DOutlookBar tall = new DOutlookBar();
        DOutlookBar.Group many = tall.addGroup("Reports");
        for (int i = 1; i <= 20; i++) {
            many.addItem("Report " + i, null, e -> { });
        }
        JScrollPane scroller = scrollerOf(tall);
        int itemWidth = scroller.getViewport().getView().getPreferredSize().width;
        int barWidth = scroller.getVerticalScrollBar().getPreferredSize().width;

        // Room for the scroll bar is reserved, so items keep their width when it appears.
        assertThat(scroller.getPreferredSize().width).isGreaterThanOrEqualTo(itemWidth + barWidth);

        tall.setSize(tall.getPreferredSize().width, 300);
        layoutTree(tall);
        assertThat(scroller.getVerticalScrollBar().isVisible()).isTrue();
        assertThat(scroller.getViewport().getWidth()).isGreaterThanOrEqualTo(itemWidth);
    }

    @Test
    void shortGroupHasNoScrollBar() {
        JScrollPane scroller = scrollerOf(bar);
        bar.setSize(bar.getPreferredSize().width, 600);
        layoutTree(bar);

        assertThat(scroller.getVerticalScrollBar().isVisible()).isFalse();
    }

    @Test
    void scrollBarsAreStyledToMatchTheContentBackground() {
        JScrollBar vertical = scrollerOf(bar).getVerticalScrollBar();
        assertThat(vertical.getClientProperty(FlatClientProperties.SCROLL_BAR_SHOW_BUTTONS)).isEqualTo(true);
        assertThat((String) vertical.getClientProperty(FlatClientProperties.STYLE)).doesNotContain("thumb");

        bar.applyClassicStyle();
        assertThat((String) vertical.getClientProperty(FlatClientProperties.STYLE)).contains("thumb: #");

        bar.applyThemeStyle();
        assertThat((String) vertical.getClientProperty(FlatClientProperties.STYLE)).doesNotContain("thumb");
    }

    private static JScrollPane scrollerOf(DOutlookBar outlook) {
        return findScroller(outlook);
    }

    private static JScrollPane findScroller(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JScrollPane sp && k.isVisible()) {
                return sp;
            }
            if (k instanceof Container cc) {
                JScrollPane found = findScroller(cc);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void layoutTree(Component c) {
        c.doLayout();
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
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
