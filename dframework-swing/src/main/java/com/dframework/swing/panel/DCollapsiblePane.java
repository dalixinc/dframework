package com.dframework.swing.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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

    /**
     * Puts {@code main} above {@code details} in a split the user can drag.
     * {@code details} get their preferred height, but never more than a third, and
     * collapsing them gives the room back to {@code main}, so the details never
     * squeeze the content they describe. The user can drag the divider; resizing
     * the window places it again.
     *
     * <pre>{@code
     * add(DCollapsiblePane.splitBelow(results, new DCollapsiblePane("Under the hood", commands)));
     * }</pre>
     */
    public static JSplitPane splitBelow(JComponent main, DCollapsiblePane details) {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, main, details);
        split.setResizeWeight(1.0);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOneTouchExpandable(false);
        main.setMinimumSize(new Dimension(0, 80));
        details.setMinimumSize(new Dimension(0, details.getHeader().getPreferredSize().height));
        details.addPropertyChangeListener(EXPANDED_PROPERTY, e -> SwingUtilities.invokeLater(() -> fitDetails(split, details)));
        // Re-place the divider when the window changes size, so a smaller window
        // shrinks both parts rather than only the main content.
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitDetails(split, details);
            }
        });
        return split;
    }

    /** Moves the divider so {@code details} gets its preferred height (up to a third), leaving {@code main} at least its minimum. */
    private static void fitDetails(JSplitPane split, DCollapsiblePane details) {
        int available = split.getHeight() - split.getDividerSize();
        if (available <= 0) {
            return;
        }
        int collapsed = details.getMinimumSize().height;
        // Never more than a third: the details describe the main content, so they mustn't crowd it out.
        int wanted = details.isExpanded()
                ? Math.max(collapsed, Math.min(details.getPreferredSize().height, available / 3))
                : collapsed;
        int mainMinimum = split.getTopComponent().getMinimumSize().height;
        split.setDividerLocation(Math.max(mainMinimum, available - wanted));
    }

    private void updateHeader() {
        header.setText((expanded ? "▾  " : "▸  ") + title);
        header.getAccessibleContext().setAccessibleName(title);
        header.getAccessibleContext().setAccessibleDescription(expanded ? "Expanded" : "Collapsed");
        header.setToolTipText(expanded ? "Hide " + title.toLowerCase() : "Show " + title.toLowerCase());
    }
}
