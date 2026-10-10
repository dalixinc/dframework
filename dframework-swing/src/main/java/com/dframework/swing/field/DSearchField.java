package com.dframework.swing.field;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

/**
 * A search box: magnifier icon, placeholder text, a clear button, and
 * <kbd>Esc</kbd> to clear.
 *
 * <p>Listeners registered with {@link #onSearch(Consumer)} are told the text
 * shortly after the user stops typing (debounced), so a large table is not
 * re-filtered on every keystroke.
 *
 * <pre>{@code
 * DSearchField search = new DSearchField("Search calls");
 * search.onSearch(table::setFilterText);
 * }</pre>
 */
public class DSearchField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_DELAY_MS = 150;

    private final transient List<Consumer<String>> listeners = new ArrayList<>();
    private final Timer debounce;
    private String lastFired = "";

    public DSearchField() {
        this("Search");
    }

    /** @param placeholder grey hint shown while empty, e.g. "Search calls" */
    public DSearchField(String placeholder) {
        super(20);
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        getAccessibleContext().setAccessibleName(placeholder);

        debounce = new Timer(DEFAULT_DELAY_MS, e -> fire());
        debounce.setRepeats(false);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "dframework.search.clear");
        getActionMap().put("dframework.search.clear", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setText("");
            }

            @Override
            public boolean accept(Object sender) {
                // Let Esc reach dialogs and palettes when there is nothing to clear.
                return !getText().isEmpty();
            }
        });
        // Enter searches straight away, without waiting for the debounce.
        addActionListener(e -> fire());
    }

    /** Calls {@code listener} with the text after the user pauses typing (and on Enter or clear). */
    public void onSearch(Consumer<String> listener) {
        listeners.add(listener);
    }

    /** Pause before notifying listeners, in milliseconds; 0 notifies on every change. Default 150. */
    public void setSearchDelay(int millis) {
        debounce.setInitialDelay(millis);
        debounce.setDelay(millis);
    }

    public int getSearchDelay() {
        return debounce.getInitialDelay();
    }

    private void changed() {
        // Clearing should feel instant; typing waits for a pause.
        if (getText().isEmpty() || debounce.getInitialDelay() == 0) {
            fire();
        } else {
            debounce.restart();
        }
    }

    private void fire() {
        debounce.stop();
        String text = getText();
        if (text.equals(lastFired)) {
            return;
        }
        lastFired = text;
        listeners.forEach(listener -> listener.accept(text));
    }
}
