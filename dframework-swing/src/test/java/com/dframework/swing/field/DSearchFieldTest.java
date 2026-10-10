package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class DSearchFieldTest {

    private final DSearchField field = new DSearchField("Search calls");
    private final List<String> searches = new ArrayList<>();

    @Test
    void notifiesImmediatelyWithZeroDelay() {
        field.setSearchDelay(0);
        field.onSearch(searches::add);

        field.setText("acme");

        assertThat(searches).containsExactly("acme");
    }

    @Test
    void debouncesTyping() throws Exception {
        field.setSearchDelay(100);
        field.onSearch(searches::add);

        SwingUtilities.invokeAndWait(() -> {
            field.setText("a");
            field.setText("ac");
            field.setText("acme");
        });
        assertThat(searches).isEmpty();

        Thread.sleep(400);
        SwingUtilities.invokeAndWait(() -> { });
        assertThat(searches).containsExactly("acme");
    }

    @Test
    void escapeClearsAndReportsAtOnce() {
        field.onSearch(searches::add);
        field.setSearchDelay(0);
        field.setText("acme");

        var clear = field.getActionMap().get(field.getInputMap().get(KeyStroke.getKeyStroke("ESCAPE")));
        assertThat(clear.accept(null)).isTrue();
        clear.actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, null));

        assertThat(field.getText()).isEmpty();
        assertThat(searches).containsExactly("acme", "");
        assertThat(clear.accept(null)).as("Esc passes through when already empty").isFalse();
    }
}
