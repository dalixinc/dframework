package com.dframework.swing.nav;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * An Outlook-style navigation bar: a stack of group headers, with the open
 * group's items shown as large icons with captions beneath.
 *
 * <p>Clicking a header opens that group; the headers after it move to the
 * bottom of the bar. Clicking an item selects it and runs its action, which is
 * typically to show a screen in the main area of the window.
 *
 * <pre>{@code
 * DOutlookBar bar = new DOutlookBar();
 * DOutlookBar.Group logging = bar.addGroup("Logging");
 * logging.addItem("Log Call", logIcon, e -> cards.show(main, "log"));
 * logging.addItem("Time\nSheet", sheetIcon, e -> cards.show(main, "time"));
 * bar.addGroup("Reports").addItem(...);
 * frame.add(bar, BorderLayout.WEST);
 * }</pre>
 *
 * <p>Colours follow the current theme unless set with
 * {@link #setContentBackground(Color)} and {@link #setItemForeground(Color)};
 * {@link #applyClassicStyle()} recreates the Windows XP look of the original
 * 2005 Tracker 2 bar.
 */
public class DOutlookBar extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Bound property fired when a different group is opened. */
    public static final String SELECTED_GROUP_PROPERTY = "selectedGroup";

    /** Bound property fired when a different item is selected. */
    public static final String SELECTED_ITEM_PROPERTY = "selectedItem";

    private static final Color CLASSIC_CONTENT = new Color(0xACA899);
    private static final Color CLASSIC_HEADER = new Color(0xECE9D8);

    private final transient List<Group> groups = new ArrayList<>();
    private final JPanel topHeaders = verticalStack();
    private final JPanel bottomHeaders = verticalStack();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new ContentPanel(cards);
    private final ButtonGroup itemSelection = new ButtonGroup();

    private transient Group selectedGroup;
    private transient Item selectedItem;
    private Color contentBackground;
    private Color itemForeground;
    private Color headerBackground;

    public DOutlookBar() {
        super(new BorderLayout());
        add(topHeaders, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(bottomHeaders, BorderLayout.SOUTH);
    }

    /** Adds a group at the bottom of the bar. The first group added is opened. */
    public Group addGroup(String title) {
        Group group = new Group(title);
        groups.add(group);
        content.add(group.scroller, group.cardName);
        if (selectedGroup == null) {
            setSelectedGroup(group);
        } else {
            layoutHeaders();
        }
        return group;
    }

    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /** The open group, if any groups have been added. */
    public Optional<Group> getSelectedGroup() {
        return Optional.ofNullable(selectedGroup);
    }

    /** Opens {@code group}, moving the headers below it to the bottom of the bar. */
    public void setSelectedGroup(Group group) {
        requireOwn(group);
        Group old = selectedGroup;
        selectedGroup = group;
        cards.show(content, group.cardName);
        layoutHeaders();
        firePropertyChange(SELECTED_GROUP_PROPERTY, old, group);
    }

    /** The selected item, if any. */
    public Optional<Item> getSelectedItem() {
        return Optional.ofNullable(selectedItem);
    }

    /**
     * Selects {@code item} as if the user had clicked it: its group is opened
     * and its action runs.
     */
    public void setSelectedItem(Item item) {
        requireOwn(item.group());
        if (selectedGroup != item.group()) {
            setSelectedGroup(item.group());
        }
        item.button.setSelected(true);
        Item old = selectedItem;
        selectedItem = item;
        item.button.scrollRectToVisible(new Rectangle(item.button.getSize()));
        firePropertyChange(SELECTED_ITEM_PROPERTY, old, item);
        item.listener.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, item.text));
    }

    /** Background behind the items, or {@code null} for a shade derived from the theme. */
    public Color getContentBackground() {
        return contentBackground;
    }

    public void setContentBackground(Color color) {
        Color old = contentBackground;
        contentBackground = color;
        firePropertyChange("contentBackground", old, color);
        repaint();
    }

    /** Colour of item captions and highlights, or {@code null} for the theme's label colour. */
    public Color getItemForeground() {
        return itemForeground;
    }

    public void setItemForeground(Color color) {
        Color old = itemForeground;
        itemForeground = color;
        firePropertyChange("itemForeground", old, color);
        repaint();
    }

    /** Background of the group headers, or {@code null} for the theme's button colour. */
    public Color getHeaderBackground() {
        return headerBackground;
    }

    public void setHeaderBackground(Color color) {
        Color old = headerBackground;
        headerBackground = color;
        groups.forEach(g -> g.header.setBackground(color));
        firePropertyChange("headerBackground", old, color);
    }

    /** Recreates the look of the original Tracker 2 bar: white captions on Windows XP khaki. */
    public void applyClassicStyle() {
        setContentBackground(CLASSIC_CONTENT);
        setItemForeground(Color.WHITE);
        setHeaderBackground(CLASSIC_HEADER);
    }

    /** Goes back to colours taken from the current theme. */
    public void applyThemeStyle() {
        setContentBackground(null);
        setItemForeground(null);
        setHeaderBackground(null);
    }

    private Color effectiveContentBackground() {
        if (contentBackground != null) {
            return contentBackground;
        }
        Color panel = UIManager.getColor("Panel.background");
        if (panel == null) {
            return Color.LIGHT_GRAY;
        }
        // A shade away from the panel colour, so the bar reads as its own area.
        boolean dark = (panel.getRed() + panel.getGreen() + panel.getBlue()) / 3 < 128;
        return dark ? shade(panel, 1.25f) : shade(panel, 0.92f);
    }

    private Color effectiveItemForeground() {
        if (itemForeground != null) {
            return itemForeground;
        }
        Color label = UIManager.getColor("Label.foreground");
        return label != null ? label : Color.BLACK;
    }

    private static Color shade(Color c, float factor) {
        return new Color(
                Math.min(255, Math.round(c.getRed() * factor)),
                Math.min(255, Math.round(c.getGreen() * factor)),
                Math.min(255, Math.round(c.getBlue() * factor)));
    }

    private void layoutHeaders() {
        topHeaders.removeAll();
        bottomHeaders.removeAll();
        boolean afterSelected = false;
        for (Group group : groups) {
            (afterSelected ? bottomHeaders : topHeaders).add(group.header);
            if (group == selectedGroup) {
                afterSelected = true;
            }
        }
        revalidate();
        repaint();
    }

    private void requireOwn(Group group) {
        if (!groups.contains(Objects.requireNonNull(group, "group"))) {
            throw new IllegalArgumentException("Group " + group.title + " belongs to another bar");
        }
    }

    private static JPanel verticalStack() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /** A titled group of items. */
    public final class Group {

        private final String title;
        private final String cardName;
        private final JButton header;
        private final ItemList list = new ItemList();
        private final JScrollPane scroller;
        private final List<Item> items = new ArrayList<>();

        private Group(String title) {
            this.title = Objects.requireNonNull(title, "title");
            this.cardName = "group-" + System.identityHashCode(this);
            header = new HeaderButton(title);
            header.setBackground(headerBackground);
            header.addActionListener(e -> setSelectedGroup(this));
            scroller = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            scroller.setOpaque(false);
            scroller.getViewport().setOpaque(false);
            scroller.getVerticalScrollBar().setUnitIncrement(16);
        }

        public String getTitle() {
            return title;
        }

        public List<Item> getItems() {
            return Collections.unmodifiableList(items);
        }

        /**
         * Adds an item. Use {@code \n} in {@code text} to break a long caption
         * over two lines, e.g. {@code "Customer\nGroups"}.
         *
         * @param listener called when the item is selected; its source is the {@link Item}
         */
        public Item addItem(String text, Icon icon, ActionListener listener) {
            Item item = new Item(this, text, icon, listener);
            items.add(item);
            itemSelection.add(item.button);
            list.add(item.button);
            list.add(Box.createVerticalStrut(4));
            DOutlookBar.this.revalidate();
            return item;
        }

        /** Adds an item from an {@link Action}, using its name and large (or small) icon. */
        public Item addItem(Action action) {
            Icon icon = (Icon) Optional.ofNullable(action.getValue(Action.LARGE_ICON_KEY))
                    .orElse(action.getValue(Action.SMALL_ICON));
            return addItem((String) action.getValue(Action.NAME), icon, action);
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /** One selectable entry in a {@link Group}. */
    public final class Item {

        private final Group group;
        private final String text;
        private final ActionListener listener;
        private final ItemButton button;

        private Item(Group group, String text, Icon icon, ActionListener listener) {
            this.group = group;
            this.text = Objects.requireNonNull(text, "text");
            this.listener = Objects.requireNonNull(listener, "listener");
            button = new ItemButton(text, icon);
            button.addActionListener(e -> setSelectedItem(this));
        }

        public Group group() {
            return group;
        }

        public String getText() {
            return text;
        }

        public Icon getIcon() {
            return button.getIcon();
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Full-width group header. */
    private static final class HeaderButton extends JButton {

        @Serial
        private static final long serialVersionUID = 1L;

        HeaderButton(String title) {
            super(title);
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Large icon with its caption underneath; highlights on hover and when selected. */
    private final class ItemButton extends JToggleButton {

        @Serial
        private static final long serialVersionUID = 1L;

        ItemButton(String text, Icon icon) {
            super(captionHtml(text), icon);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setHorizontalAlignment(SwingConstants.CENTER);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            getAccessibleContext().setAccessibleName(text.replace('\n', ' '));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        public Color getForeground() {
            return effectiveItemForeground();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int alpha = model.isPressed() ? 90 : isSelected() ? 60 : model.isRollover() || hasFocus() ? 30 : 0;
            if (alpha > 0) {
                Color base = effectiveItemForeground();
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                    g2.fillRoundRect(3, 1, getWidth() - 6, getHeight() - 2, 10, 10);
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }

    private static String captionHtml(String text) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        return "<html><div style='text-align:center'>" + escaped + "</div></html>";
    }

    /** Vertical list of items that tracks the viewport width, so it never scrolls sideways. */
    private static final class ItemList extends JPanel implements Scrollable {

        @Serial
        private static final long serialVersionUID = 1L;

        ItemList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Paints the item area background. */
    private final class ContentPanel extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        ContentPanel(CardLayout layout) {
            super(layout);
        }

        @Override
        public Color getBackground() {
            return effectiveContentBackground();
        }
    }
}
