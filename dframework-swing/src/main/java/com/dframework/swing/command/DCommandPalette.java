package com.dframework.swing.command;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.dframework.swing.nav.DOutlookBar;
import com.formdev.flatlaf.FlatClientProperties;

/**
 * A searchable list of everything the user can do, opened with
 * <kbd>Ctrl+Shift+P</kbd>, as in modern editors.
 *
 * <p>Type part of a command's name and it is found by fuzzy matching ("fdark"
 * finds "Theme › FlatLaf Dark"); <kbd>Enter</kbd> runs it. Recently used
 * commands come first.
 *
 * <pre>{@code
 * DCommandPalette palette = DCommandPalette.install(frame);   // already knows every menu item
 * palette.addOutlookBar(navBar);                             // "Go to" every screen
 * palette.addCommand(Command.of("Data", "Refresh", this::refresh));
 * }</pre>
 *
 * <p>Out of the box it includes every enabled item of the window's menu bar,
 * read afresh each time it opens, with menu path and shortcut shown.
 */
public final class DCommandPalette {

    /** Ctrl+Shift+P. */
    public static final KeyStroke DEFAULT_KEY_STROKE =
            KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    private static final String CLIENT_KEY = DCommandPalette.class.getName();
    private static final String TOGGLE = "dframework.commandPalette.toggle";
    private static final String PATH_SEPARATOR = " › ";
    private static final int MAX_RECENT = 5;
    private static final double RECENT_BONUS = 3;
    private static final int ICON_SIZE = 16;

    private final JRootPane root;
    private final List<Command> commands = new ArrayList<>();
    private final List<Supplier<? extends Collection<Command>>> sources = new ArrayList<>();
    private final Deque<String> recent = new ArrayDeque<>();
    private KeyStroke keyStroke;
    private boolean includeMenuBar = true;
    private int maxVisibleRows = 10;
    private Overlay overlay;

    private DCommandPalette(JRootPane root, KeyStroke keyStroke) {
        this.root = root;
        setKeyStroke(keyStroke);
        root.getActionMap().put(TOGGLE, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isOpen()) {
                    close();
                } else {
                    open();
                }
            }
        });
    }

    /** Installs a palette on {@code window}, opened with {@link #DEFAULT_KEY_STROKE}. */
    public static DCommandPalette install(RootPaneContainer window) {
        return install(window.getRootPane(), DEFAULT_KEY_STROKE);
    }

    /**
     * Installs a palette on the window owning {@code root}, or returns the one
     * already there.
     */
    public static DCommandPalette install(JRootPane root, KeyStroke keyStroke) {
        Objects.requireNonNull(root, "root");
        if (root.getClientProperty(CLIENT_KEY) instanceof DCommandPalette existing) {
            existing.setKeyStroke(keyStroke);
            return existing;
        }
        DCommandPalette palette = new DCommandPalette(root, keyStroke);
        root.putClientProperty(CLIENT_KEY, palette);
        return palette;
    }

    /** The palette installed on {@code component}'s window, if any. */
    public static Optional<DCommandPalette> of(Component component) {
        JRootPane root = component instanceof RootPaneContainer c ? c.getRootPane() : SwingUtilities.getRootPane(component);
        return root != null && root.getClientProperty(CLIENT_KEY) instanceof DCommandPalette p
                ? Optional.of(p) : Optional.empty();
    }

    /** Changes the shortcut that opens (and closes) the palette. */
    public void setKeyStroke(KeyStroke keyStroke) {
        Objects.requireNonNull(keyStroke, "keyStroke");
        var inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (this.keyStroke != null) {
            inputMap.remove(this.keyStroke);
        }
        this.keyStroke = keyStroke;
        inputMap.put(keyStroke, TOGGLE);
    }

    public KeyStroke getKeyStroke() {
        return keyStroke;
    }

    public DCommandPalette addCommand(Command command) {
        commands.add(Objects.requireNonNull(command, "command"));
        return this;
    }

    public DCommandPalette addCommands(Collection<Command> commands) {
        commands.forEach(this::addCommand);
        return this;
    }

    /**
     * Adds commands computed each time the palette opens, for lists that
     * change, such as open documents.
     */
    public DCommandPalette addSource(Supplier<? extends Collection<Command>> source) {
        sources.add(Objects.requireNonNull(source, "source"));
        return this;
    }

    /** Adds a "Go to" command for every item in {@code bar}. */
    public DCommandPalette addOutlookBar(DOutlookBar bar) {
        return addSource(() -> outlookBarCommands(bar));
    }

    /** Whether the window's menu bar items are included. Default true. */
    public void setIncludeMenuBar(boolean includeMenuBar) {
        this.includeMenuBar = includeMenuBar;
    }

    /** How many results show before scrolling. Default 10. */
    public void setMaxVisibleRows(int rows) {
        if (rows < 1) {
            throw new IllegalArgumentException("rows must be positive");
        }
        maxVisibleRows = rows;
    }

    public boolean isOpen() {
        return overlay != null && overlay.getParent() != null;
    }

    /** Opens the palette with an empty query. */
    public void open() {
        if (isOpen()) {
            return;
        }
        if (overlay == null) {
            overlay = new Overlay();
        }
        overlay.previousFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        JLayeredPane pane = root.getLayeredPane();
        pane.add(overlay, JLayeredPane.DRAG_LAYER);
        overlay.setBounds(0, 0, pane.getWidth(), pane.getHeight());
        overlay.search.setText("");
        overlay.refresh();
        overlay.search.requestFocusInWindow();
    }

    /** Closes the palette without running anything, returning focus to where it was. */
    public void close() {
        if (!isOpen()) {
            return;
        }
        JLayeredPane pane = root.getLayeredPane();
        pane.remove(overlay);
        pane.repaint();
        if (overlay.previousFocus != null) {
            overlay.previousFocus.requestFocusInWindow();
        }
    }

    /** All current commands: added ones, sources, and the menu bar. */
    List<Command> allCommands() {
        List<Command> all = new ArrayList<>(commands);
        sources.forEach(source -> all.addAll(source.get()));
        if (includeMenuBar && root.getJMenuBar() != null) {
            all.addAll(menuBarCommands(root.getJMenuBar()));
        }
        return all;
    }

    /** Results for {@code query}, best first. */
    List<Result> search(String query) {
        List<Command> all = allCommands();
        List<Result> results = new ArrayList<>();
        if (query.isBlank()) {
            for (String key : recent) {
                all.stream().filter(c -> c.searchText().equals(key)).findFirst()
                        .ifPresent(c -> results.add(new Result(c, new int[0], true)));
            }
            all.stream()
                    .filter(c -> !recent.contains(c.searchText()))
                    .sorted(Comparator.comparing(Command::searchText, String.CASE_INSENSITIVE_ORDER))
                    .forEach(c -> results.add(new Result(c, new int[0], false)));
            return results;
        }
        record Scored(Result result, double score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Command command : all) {
            FuzzyMatcher.Match match = FuzzyMatcher.match(query, command.searchText());
            if (match != null) {
                boolean isRecent = recent.contains(command.searchText());
                scored.add(new Scored(new Result(command, match.positions(), isRecent),
                        match.score() + (isRecent ? RECENT_BONUS : 0)));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparingInt(s -> s.result().command().searchText().length()));
        scored.forEach(s -> results.add(s.result()));
        return results;
    }

    /** Runs {@code command} after closing the palette, and remembers it as recent. */
    void run(Command command) {
        close();
        String key = command.searchText();
        recent.remove(key);
        recent.addFirst(key);
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
        // After the palette has gone, so any dialog the command opens gets focus properly.
        SwingUtilities.invokeLater(command.action());
    }

    /** For tests: types {@code query} into the open palette. */
    void setQuery(String query) {
        overlay.search.setText(query);
    }

    /** For tests: the results currently listed. */
    List<Result> visibleResults() {
        List<Result> list = new ArrayList<>();
        for (int i = 0; i < overlay.model.size(); i++) {
            list.add(overlay.model.get(i));
        }
        return list;
    }

    /** For tests: presses Enter. */
    void runSelected() {
        overlay.runSelected();
    }

    /** Every enabled leaf item in {@code bar}, named by its menu path. */
    static List<Command> menuBarCommands(JMenuBar bar) {
        List<Command> list = new ArrayList<>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null && menu.isVisible() && menu.isEnabled()) {
                collect(menu, menu.getText(), list);
            }
        }
        return list;
    }

    private static void collect(JMenu menu, String path, List<Command> list) {
        for (Component c : menu.getMenuComponents()) {
            if (!c.isVisible() || !c.isEnabled()) {
                continue;
            }
            if (c instanceof JMenu sub) {
                collect(sub, path + PATH_SEPARATOR + sub.getText(), list);
            } else if (c instanceof JMenuItem item && item.getText() != null && !item.getText().isBlank()) {
                Boolean checked = item instanceof JCheckBoxMenuItem || item instanceof JRadioButtonMenuItem
                        ? item.isSelected() : null;
                list.add(new Command(item.getText(), path, item.getIcon(), item.getAccelerator(), checked, item::doClick));
            }
        }
    }

    static List<Command> outlookBarCommands(DOutlookBar bar) {
        List<Command> list = new ArrayList<>();
        for (DOutlookBar.Group group : bar.getGroups()) {
            for (DOutlookBar.Item item : group.getItems()) {
                list.add(new Command(item.getText().replace('\n', ' '), "Go to" + PATH_SEPARATOR + group.getTitle(),
                        item.getIcon(), null, null, () -> bar.setSelectedItem(item)));
            }
        }
        return list;
    }

    /** One row of results; positions index into the command's search text. */
    record Result(Command command, int[] positions, boolean recent) {
    }

    /** Dims the window and shows the search card near the top. */
    private final class Overlay extends JComponent {

        @Serial
        private static final long serialVersionUID = 1L;

        private final JTextField search = new JTextField();
        private final DefaultListModel<Result> model = new DefaultListModel<>();
        private final JList<Result> list = new JList<>(model);
        private final JScrollPane scroller = new JScrollPane(list);
        private final JLabel empty = new JLabel("No matching commands");
        private final JPanel card = new JPanel(new BorderLayout(0, 6));
        private transient Component previousFocus;

        Overlay() {
            setLayout(null);
            setOpaque(false);

            search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Type a command…");
            search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
            search.getAccessibleContext().setAccessibleName("Command palette search");
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    refresh();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    refresh();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    refresh();
                }
            });
            bind("UP", () -> move(-1));
            bind("DOWN", () -> move(1));
            bind("PAGE_UP", () -> move(-maxVisibleRows));
            bind("PAGE_DOWN", () -> move(maxVisibleRows));
            bind("ENTER", this::runSelected);
            bind("ESCAPE", DCommandPalette.this::close);

            list.setFocusable(false);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new ResultRenderer());
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        list.setSelectedIndex(index);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    runSelected();
                }
            };
            list.addMouseListener(mouse);
            list.addMouseMotionListener(mouse);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            empty.setEnabled(false);
            empty.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            Color border = UIManager.getColor("Component.borderColor");
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border != null ? border : Color.GRAY),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            card.add(search, BorderLayout.NORTH);
            card.add(scroller, BorderLayout.CENTER);
            card.add(empty, BorderLayout.SOUTH);
            add(card);

            // Clicking the dimmed area outside the card closes the palette.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    close();
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    layoutCard();
                }
            });
            root.getLayeredPane().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (isOpen()) {
                        JLayeredPane pane = root.getLayeredPane();
                        setBounds(0, 0, pane.getWidth(), pane.getHeight());
                    }
                }
            });
        }

        private void bind(String key, Runnable action) {
            search.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), "palette." + key);
            search.getActionMap().put("palette." + key, new AbstractAction() {
                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }

        void refresh() {
            model.clear();
            search(search.getText()).forEach(model::addElement);
            if (!model.isEmpty()) {
                list.setSelectedIndex(0);
                list.ensureIndexIsVisible(0);
            }
            list.setVisibleRowCount(Math.max(1, Math.min(model.size(), maxVisibleRows)));
            scroller.setVisible(!model.isEmpty());
            empty.setVisible(model.isEmpty());
            layoutCard();
        }

        private void layoutCard() {
            int width = Math.min(640, Math.max(200, getWidth() - 40));
            card.setSize(width, Integer.MAX_VALUE);
            int height = Math.min(card.getPreferredSize().height, Math.max(80, getHeight() - 80));
            card.setBounds((getWidth() - width) / 2, Math.min(48, getHeight() / 10), width, height);
            card.revalidate();
            repaint();
        }

        private void move(int delta) {
            if (model.isEmpty()) {
                return;
            }
            int index = Math.floorMod(list.getSelectedIndex() + delta, model.size());
            if (Math.abs(delta) > 1) {
                index = Math.max(0, Math.min(model.size() - 1, list.getSelectedIndex() + delta));
            }
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }

        void runSelected() {
            Result selected = list.getSelectedValue();
            if (selected != null) {
                run(selected.command());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    /** Icon, highlighted "category › name", and shortcut on the right. */
    private static final class ResultRenderer extends JPanel implements ListCellRenderer<Result> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final JLabel text = new JLabel();
        private final JLabel right = new JLabel();

        ResultRenderer() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            text.setIconTextGap(8);
            add(text, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Result> list, Result value, int index,
                boolean selected, boolean focused) {
            Color bg = selected ? list.getSelectionBackground() : list.getBackground();
            Color fg = selected ? list.getSelectionForeground() : list.getForeground();
            Color dim = mix(fg, bg, 0.45f);
            setBackground(bg);
            text.setForeground(fg);
            right.setForeground(dim);

            Command command = value.command();
            text.setIcon(command.icon() == null ? new BlankIcon() : new FittedIcon(command.icon(), ICON_SIZE));
            text.setText(html(command, value.positions(), dim));

            StringBuilder hint = new StringBuilder();
            if (Boolean.TRUE.equals(command.checked())) {
                hint.append("✓  ");
            }
            if (value.recent() && value.positions().length == 0) {
                hint.append("recently used  ");
            }
            if (command.shortcut() != null) {
                hint.append(describe(command.shortcut()));
            }
            right.setText(hint.toString());
            return this;
        }

        /** "category › name" with the category dimmed and matched characters in bold. */
        private static String html(Command command, int[] positions, Color dim) {
            String full = command.searchText();
            int nameStart = full.length() - command.name().length();
            StringBuilder html = new StringBuilder("<html>");
            if (nameStart > 0) {
                html.append("<span style='color:").append(hex(dim)).append("'>");
                appendHighlighted(html, full, 0, nameStart, positions);
                html.append("</span>");
            }
            appendHighlighted(html, full, nameStart, full.length(), positions);
            return html.append("</html>").toString();
        }

        private static void appendHighlighted(StringBuilder html, String text, int from, int to, int[] positions) {
            for (int i = from; i < to; i++) {
                boolean hit = Arrays.binarySearch(positions, i) >= 0;
                if (hit) {
                    html.append("<b>");
                }
                html.append(escape(text.charAt(i)));
                if (hit) {
                    html.append("</b>");
                }
            }
        }

        private static String escape(char c) {
            return switch (c) {
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '&' -> "&amp;";
                default -> String.valueOf(c);
            };
        }

        static String describe(KeyStroke ks) {
            String modifiers = InputEvent.getModifiersExText(ks.getModifiers());
            String key = KeyEvent.getKeyText(ks.getKeyCode());
            return modifiers.isEmpty() ? key : modifiers + "+" + key;
        }

        private static String hex(Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }

        private static Color mix(Color a, Color b, float amount) {
            return new Color(
                    Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
        }
    }

    /** Keeps rows aligned when a command has no icon. */
    private static final class BlankIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return ICON_SIZE;
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }
    }

    /** Scales a larger icon (such as a 32px menu icon) down to fit the row. */
    private record FittedIcon(Icon icon, int size) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            double scale = Math.min(1.0, (double) size / Math.max(icon.getIconWidth(), icon.getIconHeight()));
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.translate(x + (size - icon.getIconWidth() * scale) / 2, y + (size - icon.getIconHeight() * scale) / 2);
                g2.scale(scale, scale);
                icon.paintIcon(c, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
