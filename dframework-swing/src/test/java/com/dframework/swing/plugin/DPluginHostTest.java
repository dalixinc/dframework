package com.dframework.swing.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.nav.DOutlookBar;

class DPluginHostTest {

    private final AuditLog audit = AuditLog.inMemory();
    private final DPluginHost host = new DPluginHost(audit);

    @BeforeEach
    void reset() {
        TestPlugins.Good.created = 0;
        TestPlugins.Good.shown = 0;
        TestPlugins.Good.disposed = 0;
    }

    @Test
    void discoversPluginsFromServiceFilesSortedByName() {
        List<DPlugin> plugins = DPluginHost.discover();

        assertThat(plugins).extracting(DPlugin::name).containsSubsequence("Broken Plugin", "Good Plugin");
    }

    @Test
    void viewIsCreatedOnlyWhenNeeded() {
        DPluginPane pane = host.mount(new TestPlugins.Good());
        assertThat(pane.isStarted()).isFalse();
        assertThat(TestPlugins.Good.created).isZero();

        pane.start();
        pane.start();

        assertThat(TestPlugins.Good.created).isEqualTo(1);
        assertThat(pane.getView()).get().isInstanceOf(JLabel.class);
        assertThat(audit.recent("test.good")).extracting(AuditLog.Entry::action).containsExactly("hello", "start");
    }

    @Test
    void aFailingPluginShowsItsErrorAndTheHostCarriesOn() {
        DPluginPane broken = host.mount(new TestPlugins.Broken());
        DPluginPane good = host.mount(new TestPlugins.Good());

        broken.start();
        good.start();

        assertThat(broken.isFailed()).isTrue();
        assertThat(broken.getView()).isEmpty();
        assertThat(contains(broken, JScrollPane.class)).as("error panel shown in place of the plugin").isTrue();
        assertThat(audit.recent("test.broken")).singleElement()
                .satisfies(e -> assertThat(e.details().get("error")).contains("database driver missing"));
        assertThat(good.isFailed()).isFalse();
    }

    @Test
    void disposeAllStopsStartedPluginsOnce() {
        DPluginPane started = host.mount(new TestPlugins.Good());
        host.mount(new TestPlugins.Good());   // never shown, so never started
        started.start();

        host.disposeAll();
        host.disposeAll();

        assertThat(TestPlugins.Good.disposed).isEqualTo(1);
    }

    @Test
    void fillsAnOutlookBarByCategory() {
        DOutlookBar bar = new DOutlookBar();
        bar.addGroup("Tests");
        DPluginPane[] shown = new DPluginPane[1];

        List<DPluginPane> panes = host.addToOutlookBar(bar,
                List.of(new TestPlugins.Good(), new DPlugin() {
                    public String id() { return "other"; }
                    public String name() { return "Other"; }
                    public JComponent createView(PluginContext c) { return new JLabel(); }
                }), pane -> shown[0] = pane);

        assertThat(bar.getGroups()).extracting(DOutlookBar.Group::getTitle).containsExactly("Tests", "Plugins");
        bar.setSelectedItem(bar.getGroups().get(0).getItems().get(0));
        assertThat(shown[0]).isSameAs(panes.get(0));
    }

    @Test
    void rejectsIdsThatCannotNameSettings() {
        DPlugin bad = new DPlugin() {
            public String id() { return "has/slash"; }
            public String name() { return "Bad"; }
            public JComponent createView(PluginContext c) { return new JLabel(); }
        };

        assertThatThrownBy(() -> host.mount(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    private static boolean contains(Component root, Class<?> type) {
        if (type.isInstance(root)) {
            return true;
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                if (contains(child, type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
