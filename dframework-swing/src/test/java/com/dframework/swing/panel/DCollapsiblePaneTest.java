package com.dframework.swing.panel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;

class DCollapsiblePaneTest {

    private final JLabel content = new JLabel("details");
    private final DCollapsiblePane pane = new DCollapsiblePane("Under the hood", content);

    @Test
    void clickingTheHeaderFoldsAndUnfolds() {
        assertThat(pane.isExpanded()).isTrue();
        assertThat(content.isVisible()).isTrue();

        pane.getHeader().doClick();
        assertThat(pane.isExpanded()).isFalse();
        assertThat(content.isVisible()).isFalse();
        assertThat(pane.getHeader().getText()).startsWith("▸").endsWith("Under the hood");

        pane.getHeader().doClick();
        assertThat(content.isVisible()).isTrue();
        assertThat(pane.getHeader().getText()).startsWith("▾");
    }

    @Test
    void firesExpandedChanges() {
        List<Object> changes = new ArrayList<>();
        pane.addPropertyChangeListener(DCollapsiblePane.EXPANDED_PROPERTY, e -> changes.add(e.getNewValue()));

        pane.setExpanded(false);
        pane.setExpanded(false);
        pane.setExpanded(true);

        assertThat(changes).containsExactly(false, true);
    }

    @Test
    void remembersStateBetweenRuns() throws Exception {
        Preferences node = Preferences.userRoot().node("com/dframework/test/" + UUID.randomUUID());
        try {
            pane.rememberState(node, "open");
            pane.setExpanded(false);

            DCollapsiblePane later = new DCollapsiblePane("Under the hood", new JLabel());
            later.rememberState(node, "open");
            assertThat(later.isExpanded()).isFalse();
        } finally {
            Preferences parent = node.parent();
            node.removeNode();
            if (parent.childrenNames().length == 0) {
                parent.removeNode();
            }
        }
    }

    @Test
    void splitBelowGivesTheExtraSpaceToTheMainComponent() {
        JPanel main = new JPanel();
        JSplitPane split = DCollapsiblePane.splitBelow(main, pane);
        assertThat(split.getTopComponent()).isSameAs(main);
        assertThat(split.getBottomComponent()).isSameAs(pane);
        assertThat(split.getResizeWeight()).isEqualTo(1.0);
        assertThat(pane.getMinimumSize().height).isEqualTo(pane.getHeader().getPreferredSize().height);
    }
}
