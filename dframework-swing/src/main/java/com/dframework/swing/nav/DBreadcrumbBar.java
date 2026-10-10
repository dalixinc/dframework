package com.dframework.swing.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.font.TextAttribute;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

/**
 * A breadcrumb trail, such as {@code Home › Reports › Customers}. Every crumb
 * but the last is a link back to that level.
 *
 * <pre>{@code
 * DBreadcrumbBar crumbs = new DBreadcrumbBar();
 * crumbs.setPath(Crumb.of("Home", home), Crumb.of("Reports", reports));
 * crumbs.push(Crumb.of("Customers", customers));
 * crumbs.addCrumbListener(crumb -> show(crumb.value()));
 * }</pre>
 *
 * <p>Clicking a crumb trims the path back to it (see
 * {@link #setTruncateOnClick(boolean)}) and tells the listeners. When the
 * path is too long for the space, middle crumbs fold into a "…" menu; the
 * first and last always stay visible.
 */
public class DBreadcrumbBar extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Bound property fired when the path changes. */
    public static final String PATH_PROPERTY = "path";

    /** One level of the trail. {@code value} is yours, e.g. the screen it stands for. */
    public record Crumb(String text, Icon icon, Object value) {

        public Crumb {
            Objects.requireNonNull(text, "text");
        }

        public static Crumb of(String text) {
            return new Crumb(text, null, null);
        }

        public static Crumb of(String text, Object value) {
            return new Crumb(text, null, value);
        }
    }

    private final transient List<Crumb> path = new ArrayList<>();
    private final transient List<Consumer<Crumb>> listeners = new CopyOnWriteArrayList<>();
    private final transient List<Component> crumbViews = new ArrayList<>();
    private final transient List<JLabel> separators = new ArrayList<>();
    private final JButton ellipsis = new LinkButton("…");
    private final JLabel ellipsisSeparator = new JLabel();
    private String separator = "›";
    private boolean truncateOnClick = true;
    /** Indices of crumbs folded into the "…" menu by the last layout. */
    private int hiddenFrom = -1;
    private int hiddenTo = -1;

    public DBreadcrumbBar() {
        super(null);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        getAccessibleContext().setAccessibleName("Breadcrumb");
        ellipsis.setToolTipText("Show hidden levels");
        ellipsis.addActionListener(e -> showHiddenMenu());
        rebuild();
    }

    /** The current path, first to last. */
    public List<Crumb> getPath() {
        return Collections.unmodifiableList(new ArrayList<>(path));
    }

    public void setPath(List<Crumb> crumbs) {
        List<Crumb> old = getPath();
        path.clear();
        path.addAll(crumbs);
        rebuild();
        firePropertyChange(PATH_PROPERTY, old, getPath());
    }

    public void setPath(Crumb... crumbs) {
        setPath(List.of(crumbs));
    }

    /** Adds a crumb at the end. */
    public void push(Crumb crumb) {
        List<Crumb> next = new ArrayList<>(path);
        next.add(Objects.requireNonNull(crumb, "crumb"));
        setPath(next);
    }

    /** Removes and returns the last crumb, or {@code null} if the path is empty. */
    public Crumb pop() {
        if (path.isEmpty()) {
            return null;
        }
        List<Crumb> next = new ArrayList<>(path);
        Crumb last = next.remove(next.size() - 1);
        setPath(next);
        return last;
    }

    /** Keeps crumbs {@code 0..index} and drops the rest. */
    public void truncate(int index) {
        if (index < 0 || index >= path.size()) {
            throw new IndexOutOfBoundsException(index);
        }
        setPath(new ArrayList<>(path.subList(0, index + 1)));
    }

    /** Called when the user clicks a crumb (after the path is trimmed, if that is on). */
    public void addCrumbListener(Consumer<Crumb> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeCrumbListener(Consumer<Crumb> listener) {
        listeners.remove(listener);
    }

    /** Whether clicking a crumb trims the path back to it. Default true. */
    public void setTruncateOnClick(boolean truncateOnClick) {
        this.truncateOnClick = truncateOnClick;
    }

    public boolean isTruncateOnClick() {
        return truncateOnClick;
    }

    /** The text between crumbs. Default "›". */
    public void setSeparator(String separator) {
        this.separator = Objects.requireNonNull(separator, "separator");
        rebuild();
    }

    /** Clicks crumb {@code index}, as the user would. */
    public void select(int index) {
        Crumb crumb = path.get(index);
        if (truncateOnClick) {
            truncate(index);
        }
        listeners.forEach(listener -> listener.accept(crumb));
    }

    /** Whether some crumbs are currently folded into the "…" menu. */
    public boolean isCollapsed() {
        return hiddenFrom >= 0;
    }

    private void rebuild() {
        removeAll();
        crumbViews.clear();
        separators.clear();
        for (int i = 0; i < path.size(); i++) {
            Crumb crumb = path.get(i);
            Component view;
            if (i == path.size() - 1) {
                JLabel current = new JLabel(crumb.text(), crumb.icon(), JLabel.LEADING);
                current.setFont(current.getFont().deriveFont(Font.BOLD));
                current.getAccessibleContext().setAccessibleDescription("Current level");
                view = current;
            } else {
                int index = i;
                JButton link = new LinkButton(crumb.text());
                link.setIcon(crumb.icon());
                link.addActionListener(e -> select(index));
                view = link;
            }
            crumbViews.add(view);
            add(view);
            if (i < path.size() - 1) {
                JLabel sep = separatorLabel();
                separators.add(sep);
                add(sep);
            }
        }
        ellipsisSeparator.setText(separator);
        styleSeparator(ellipsisSeparator);
        add(ellipsis);
        add(ellipsisSeparator);
        revalidate();
        repaint();
    }

    private JLabel separatorLabel() {
        JLabel sep = new JLabel(separator);
        styleSeparator(sep);
        return sep;
    }

    private static void styleSeparator(JLabel sep) {
        sep.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        sep.setEnabled(false);
    }

    @Override
    public Dimension getPreferredSize() {
        Insets in = getInsets();
        int width = 0;
        int height = 0;
        for (int i = 0; i < crumbViews.size(); i++) {
            Dimension d = crumbViews.get(i).getPreferredSize();
            width += d.width;
            height = Math.max(height, d.height);
            if (i < separators.size()) {
                width += separators.get(i).getPreferredSize().width;
            }
        }
        if (height == 0) {
            height = new JLabel("X").getPreferredSize().height;
        }
        return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        // Room for first › … › last, so layouts may shrink the bar and let it fold.
        Insets in = getInsets();
        int width = 0;
        if (!crumbViews.isEmpty()) {
            width += crumbViews.get(0).getPreferredSize().width;
        }
        if (crumbViews.size() > 1) {
            width += sepWidth() + crumbViews.get(crumbViews.size() - 1).getMinimumSize().width;
        }
        if (crumbViews.size() > 2) {
            width += ellipsis.getPreferredSize().width + sepWidth();
        }
        return new Dimension(width + in.left + in.right, getPreferredSize().height);
    }

    private int sepWidth() {
        return separators.isEmpty() ? ellipsisSeparator.getPreferredSize().width : separators.get(0).getPreferredSize().width;
    }

    @Override
    public void doLayout() {
        Insets in = getInsets();
        int available = getWidth() - in.left - in.right;
        int height = getHeight() - in.top - in.bottom;
        int n = crumbViews.size();

        // Fold crumbs 1, 2, 3... into "…" until the rest fits, always keeping the first and last.
        int hide = 0;
        while (n > 2 && hide < n - 2 && widthWithHidden(hide) > available) {
            hide++;
        }
        hiddenFrom = hide > 0 ? 1 : -1;
        hiddenTo = hide > 0 ? hide : -1;

        int x = in.left;
        for (int i = 0; i < n; i++) {
            Component view = crumbViews.get(i);
            boolean hidden = i >= 1 && i <= hide;
            if (hidden) {
                view.setBounds(0, 0, 0, 0);
                if (i < separators.size()) {
                    separators.get(i).setBounds(0, 0, 0, 0);
                }
                continue;
            }
            int w = view.getPreferredSize().width;
            if (i == n - 1) {
                w = Math.max(0, Math.min(w, in.left + available - x));   // last crumb shrinks with "..." if need be
            }
            view.setBounds(x, in.top, w, height);
            x += w;
            if (i < separators.size()) {
                int sw = separators.get(i).getPreferredSize().width;
                separators.get(i).setBounds(x, in.top, sw, height);
                x += sw;
            }
            if (i == 0 && hide > 0) {
                int ew = ellipsis.getPreferredSize().width;
                ellipsis.setBounds(x, in.top, ew, height);
                x += ew;
                int sw = ellipsisSeparator.getPreferredSize().width;
                ellipsisSeparator.setBounds(x, in.top, sw, height);
                x += sw;
            }
        }
        if (hide == 0) {
            ellipsis.setBounds(0, 0, 0, 0);
            ellipsisSeparator.setBounds(0, 0, 0, 0);
        }
    }

    private int widthWithHidden(int hide) {
        int width = 0;
        for (int i = 0; i < crumbViews.size(); i++) {
            if (i >= 1 && i <= hide) {
                continue;
            }
            width += crumbViews.get(i).getPreferredSize().width;
            if (i < separators.size()) {
                width += separators.get(i).getPreferredSize().width;
            }
        }
        if (hide > 0) {
            width += ellipsis.getPreferredSize().width + ellipsisSeparator.getPreferredSize().width;
        }
        return width;
    }

    private void showHiddenMenu() {
        if (hiddenFrom < 0) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        for (int i = hiddenFrom; i <= hiddenTo; i++) {
            int index = i;
            Crumb crumb = path.get(i);
            JMenuItem item = new JMenuItem(crumb.text(), crumb.icon());
            item.addActionListener(e -> select(index));
            menu.add(item);
        }
        menu.show(ellipsis, 0, ellipsis.getHeight());
    }

    /** A crumb that looks like text and turns into an underlined link on hover. */
    private static final class LinkButton extends JButton {

        @Serial
        private static final long serialVersionUID = 1L;

        LinkButton(String text) {
            super(text);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(true);
            setOpaque(false);
            setRolloverEnabled(true);
            setMargin(new Insets(0, 2, 0, 2));
            setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getModel().addChangeListener(e -> repaint());
        }

        @Override
        public Color getForeground() {
            if (getModel() != null && getModel().isRollover()) {
                Color link = UIManager.getColor("Component.linkColor");
                if (link == null) {
                    link = UIManager.getColor("Hyperlink.linkColor");
                }
                return link != null ? link : new Color(0x2F80ED);
            }
            return super.getForeground();
        }

        @Override
        public Font getFont() {
            Font font = super.getFont();
            if (font != null && getModel() != null && getModel().isRollover()) {
                return font.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
            }
            return font;
        }
    }
}
