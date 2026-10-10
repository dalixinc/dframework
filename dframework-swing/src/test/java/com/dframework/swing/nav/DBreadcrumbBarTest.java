package com.dframework.swing.nav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import com.dframework.swing.nav.DBreadcrumbBar.Crumb;

class DBreadcrumbBarTest {

    private final DBreadcrumbBar bar = new DBreadcrumbBar();
    private final List<Crumb> clicked = new ArrayList<>();

    private void path(String... texts) {
        bar.setPath(Arrays.stream(texts).map(Crumb::of).toList());
        bar.addCrumbListener(clicked::add);
    }

    private List<String> texts() {
        return bar.getPath().stream().map(Crumb::text).toList();
    }

    @Test
    void pushPopAndTruncate() {
        path("Home", "Reports");
        bar.push(Crumb.of("Customers"));
        assertThat(texts()).containsExactly("Home", "Reports", "Customers");

        assertThat(bar.pop().text()).isEqualTo("Customers");
        bar.push(Crumb.of("Calls"));
        bar.truncate(0);
        assertThat(texts()).containsExactly("Home");

        assertThatThrownBy(() -> bar.truncate(5)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void lastCrumbIsTextTheRestAreLinks() {
        path("Home", "Reports", "Customers");

        List<Component> views = visibleViews();
        assertThat(views.get(0)).isInstanceOf(AbstractButton.class);
        assertThat(views.get(views.size() - 1)).isInstanceOf(JLabel.class);
        assertThat(((JLabel) views.get(views.size() - 1)).getText()).isEqualTo("Customers");
    }

    @Test
    void clickingACrumbTrimsThePathAndNotifies() {
        path("Home", "Reports", "Customers");
        Object reports = bar.getPath().get(1);

        link("Reports").doClick();

        assertThat(texts()).containsExactly("Home", "Reports");
        assertThat(clicked).containsExactly((Crumb) reports);
    }

    @Test
    void trimmingCanBeTurnedOff() {
        path("Home", "Reports", "Customers");
        bar.setTruncateOnClick(false);

        bar.select(0);

        assertThat(texts()).hasSize(3);
        assertThat(clicked).extracting(Crumb::text).containsExactly("Home");
    }

    @Test
    void firesPathChanges() {
        List<Object> changes = new ArrayList<>();
        bar.addPropertyChangeListener(DBreadcrumbBar.PATH_PROPERTY, e -> changes.add(e.getNewValue()));

        bar.push(Crumb.of("Home"));
        bar.push(Crumb.of("Reports"));

        assertThat(changes).hasSize(2);
    }

    @Test
    void foldsMiddleCrumbsWhenNarrowKeepingFirstAndLast() {
        path("DFramework", "Tracker 2", "Reports", "Customers by region", "Call #1283");
        bar.setSize(bar.getPreferredSize());
        bar.doLayout();
        assertThat(bar.isCollapsed()).isFalse();

        bar.setSize(bar.getMinimumSize().width + 10, bar.getPreferredSize().height);
        bar.doLayout();

        assertThat(bar.isCollapsed()).isTrue();
        assertThat(link("DFramework").getWidth()).isPositive();
        assertThat(link("Tracker 2").getWidth()).isZero();
        assertThat(label("Call #1283").getWidth()).isPositive();
        assertThat(link("…").getWidth()).isPositive();
    }

    @Test
    void wideEnoughMeansNoEllipsis() {
        path("Home", "Reports");
        bar.setSize(1000, 30);
        bar.doLayout();

        assertThat(bar.isCollapsed()).isFalse();
        assertThat(link("…").getWidth()).isZero();
    }

    private List<Component> visibleViews() {
        return Arrays.stream(bar.getComponents())
                .filter(c -> c instanceof AbstractButton b && !b.getText().equals("…")
                        || c instanceof JLabel l && !l.getText().equals("›"))
                .toList();
    }

    private AbstractButton link(String text) {
        return (AbstractButton) Arrays.stream(bar.getComponents())
                .filter(c -> c instanceof AbstractButton b && b.getText().equals(text)).findFirst().orElseThrow();
    }

    private JLabel label(String text) {
        return (JLabel) Arrays.stream(bar.getComponents())
                .filter(c -> c instanceof JLabel l && l.getText().equals(text)).findFirst().orElseThrow();
    }
}
