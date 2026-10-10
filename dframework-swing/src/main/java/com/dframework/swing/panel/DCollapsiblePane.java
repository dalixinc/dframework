package com.dframework.swing.panel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.Serial;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * A titled section whose content folds away when the header is clicked, for
 * detail that some users want and others don't.
 *
 * <pre>{@code
 * DCollapsiblePane details = new DCollapsiblePane("Under the hood", commandPanel);
 * details.rememberState(prefs, "underTheHood");   // reopen as the user left it
 * }</pre>
 *
 * <p>The header is a button, so it works from the keyboard (<kbd>Space</kbd>)
 * and announces itself as expanded or collapsed to screen readers.
 */
public class DCollapsiblePane extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Bound property fired when the pane opens or closes. */
    public static final String EXPANDED_PROPERTY = "expanded";

    private final JButton header = new JButton();
    private final JComponent content;
    private String title;
    private boolean expanded;
    private transient Preferences store;
    private String storeKey;

    /** A pane that starts expanded. */
    public DCollapsiblePane(String title, JComponent content) {
        this(title, content, true);
    }

    public DCollapsiblePane(String title, JComponent content, boolean expanded) {
        super(new BorderLayout());
        this.title = Objects.requireNonNull(title, "title");
        this.content = Objects.requireNonNull(content, "content");
        header.setHorizontalAlignment(SwingConstants.LEADING);
        header.setBorderPainted(false);
        header.setContentAreaFilled(false);
        header.setFocusPainted(true);
        header.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        Font font = UIManager.getFont("Label.font");
        if (font != null) {
            header.setFont(font.deriveFont(Font.BOLD));
        }
        header.addActionListener(e -> setExpanded(!isExpanded()));
        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        this.expanded = !expanded;   // force the first update
        setExpanded(expanded);
    }

    public boolean isExpanded() {
        return expanded;
    }

    /** Shows or hides the content. */
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }
        this.expanded = expanded;
        content.setVisible(expanded);
        updateHeader();
        if (store != null) {
            store.putBoolean(storeKey, expanded);
        }
        revalidate();
        repaint();
        firePropertyChange(EXPANDED_PROPERTY, !expanded, expanded);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
        updateHeader();
    }

    /** The header button, e.g. to add a tooltip. */
    public JButton getHeader() {
        return header;
    }

    public JComponent getContent() {
        return content;
    }

    /**
     * Restores the expanded state saved under {@code key} (if any) and saves it
     * whenever it changes, so the pane reopens as the user left it.
     */
    public void rememberState(Preferences node, String key) {
        this.store = null;
        setExpanded(node.getBoolean(key, expanded));
        this.store = Objects.requireNonNull(node, "node");
        this.storeKey = Objects.requireNonNull(key, "key");
    }

    private void updateHeader() {
        header.setText((expanded ? "▾  " : "▸  ") + title);
        header.getAccessibleContext().setAccessibleName(title);
        header.getAccessibleContext().setAccessibleDescription(expanded ? "Expanded" : "Collapsed");
        header.setToolTipText(expanded ? "Hide " + title.toLowerCase() : "Show " + title.toLowerCase());
    }
}
