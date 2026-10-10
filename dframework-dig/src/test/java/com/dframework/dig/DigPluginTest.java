package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.plugin.DPlugin;
import com.dframework.swing.plugin.DPluginHost;
import com.dframework.swing.plugin.DPluginPane;

class DigPluginTest {

    // A throwaway settings root, so the tests never touch the user's real plugin settings.
    private final Preferences settings = Preferences.userRoot().node("com/dframework/test/" + UUID.randomUUID());

    @AfterEach
    void forgetSettings() throws Exception {
        Preferences parent = settings.parent();
        settings.removeNode();
        if (parent.childrenNames().length == 0) {
            parent.removeNode();
        }
    }

    @Test
    void isFoundByTheHostThroughItsServiceFile() {
        List<DPlugin> plugins = DPluginHost.discover();

        assertThat(plugins).anySatisfy(p -> {
            assertThat(p).isInstanceOf(DigPlugin.class);
            assertThat(p.name()).isEqualTo("DNS Dig");
            assertThat(p.icon()).isNotNull();
        });
    }

    @Test
    void viewBuildsAndPreviewsTheCommandAsOptionsChange() {
        DPluginHost host = new DPluginHost(AuditLog.inMemory(), settings);
        DPluginPane pane = host.mount(new DigPlugin());

        pane.start();

        assertThat(pane.isFailed()).isFalse();
        DigView view = (DigView) pane.getView().orElseThrow();
        assertThat(view.previewCommand()).isEqualTo("dig NAME A");
        assertThat(DigCommand.display(DigCommand.arguments(new DigOptions("gmail.com", "MX", "8.8.8.8",
                EnumSet.of(DigOptions.Flag.SHORT), 5)))).isEqualTo("dig @8.8.8.8 gmail.com MX +short");
    }
}
