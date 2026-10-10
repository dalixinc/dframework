package com.dframework.swing.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.nav.DOutlookBar;

/** Uses a bare JRootPane (no window), so runs headless. */
class DCommandPaletteTest {

    private final JRootPane root = new JRootPane();
    private final List<String> ran = new ArrayList<>();
    private DCommandPalette palette;

    @BeforeEach
    void setUp() throws Exception {
        root.getContentPane().add(new JPanel());
        root.setJMenuBar(menuBar());
        root.setSize(800, 600);
        root.getLayeredPane().setSize(800, 600);
        palette = onEdt(() -> DCommandPalette.install(root, DCommandPalette.DEFAULT_KEY_STROKE));
    }

    private JMenuBar menuBar() {
        JMenu file = new JMenu("File");
        JMenuItem exit = item("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        file.add(exit);
        JMenuItem disabled = item("Print");
        disabled.setEnabled(false);
        file.add(disabled);

        JMenu theme = new JMenu("Theme");
        theme.add(item("FlatLaf Light"));
        theme.add(item("FlatLaf Dark"));
        JMenu more = new JMenu("More");
        more.add(item("Kunststoff"));
        theme.add(more);
        JCheckBoxMenuItem classic = new JCheckBoxMenuItem("Classic Bar", true);
        theme.add(classic);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(theme);
        return bar;
    }

    private JMenuItem item(String text) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> ran.add(text));
        return item;
    }

    @Test
    void bindsCtrlShiftP() {
        Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(DCommandPalette.DEFAULT_KEY_STROKE);
        assertThat(key).isNotNull();
        assertThat(root.getActionMap().get(key)).isNotNull();
    }

    @Test
    void installingTwiceReturnsTheSamePalette() {
        assertThat(DCommandPalette.install(root, DCommandPalette.DEFAULT_KEY_STROKE)).isSameAs(palette);
        assertThat(DCommandPalette.of(root.getContentPane())).contains(palette);
    }

    @Test
    void harvestsEnabledMenuItemsWithPathsAndShortcuts() {
        List<Command> commands = DCommandPalette.menuBarCommands(root.getJMenuBar());

        assertThat(commands).extracting(Command::searchText).containsExactly(
                "File › Exit",
                "Theme › FlatLaf Light",
                "Theme › FlatLaf Dark",
                "Theme › More › Kunststoff",
                "Theme › Classic Bar");
        assertThat(commands.get(0).shortcut()).isEqualTo(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        assertThat(commands.get(4).checked()).isTrue();
        assertThat(commands.get(1).checked()).isNull();
    }

    @Test
    void fuzzySearchRanksTheIntendedCommandFirst() {
        assertThat(palette.search("fdark").get(0).command().name()).isEqualTo("FlatLaf Dark");
        assertThat(palette.search("kun").get(0).command().name()).isEqualTo("Kunststoff");
        assertThat(palette.search("zzz")).isEmpty();
    }

    @Test
    void typingAndEnterRunsTheTopResultAndCloses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            palette.open();
            palette.setQuery("dark");
        });
        assertThat(palette.isOpen()).isTrue();
        assertThat(palette.visibleResults().get(0).command().name()).isEqualTo("FlatLaf Dark");

        SwingUtilities.invokeAndWait(palette::runSelected);
        SwingUtilities.invokeAndWait(() -> { });   // the command runs after the palette closes

        assertThat(palette.isOpen()).isFalse();
        assertThat(ran).containsExactly("FlatLaf Dark");
    }

    @Test
    void recentlyUsedCommandsComeFirst() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            palette.open();
            palette.setQuery("kunst");
            palette.runSelected();
        });
        SwingUtilities.invokeAndWait(() -> { });

        List<DCommandPalette.Result> results = palette.search("");
        assertThat(results.get(0).command().name()).isEqualTo("Kunststoff");
        assertThat(results.get(0).recent()).isTrue();
    }

    @Test
    void addedCommandsAndOutlookBarAreSearchable() {
        palette.addCommand(Command.of("Data", "Refresh", () -> ran.add("refresh")));
        DOutlookBar bar = new DOutlookBar();
        bar.addGroup("Reports").addItem("Customer\nGroups", null, e -> ran.add("customers"));
        palette.addOutlookBar(bar);

        assertThat(palette.search("refresh").get(0).command().category()).isEqualTo("Data");
        Command goTo = palette.search("cust groups").get(0).command();
        assertThat(goTo.searchText()).isEqualTo("Go to › Reports › Customer Groups");

        goTo.action().run();
        assertThat(ran).containsExactly("customers");
    }

    @Test
    void escapeClosesWithoutRunning() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            palette.open();
            palette.close();
        });

        assertThat(palette.isOpen()).isFalse();
        assertThat(ran).isEmpty();
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }
}
