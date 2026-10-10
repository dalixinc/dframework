package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.dframework.swing.CloseAction;
import com.dframework.swing.DFrame;
import com.dframework.swing.WindowMemory;
import com.dframework.swing.command.Command;
import com.dframework.swing.command.DCommandPalette;
import com.dframework.swing.dialog.MessageIcons;
import com.dframework.swing.nav.DBreadcrumbBar;
import com.dframework.swing.nav.DBreadcrumbBar.Crumb;
import com.dframework.swing.nav.DOutlookBar;
import com.dframework.swing.notify.DToast;
import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;
import com.dframework.swing.util.DResources;

/**
 * Showcase for DFramework: an Outlook bar navigating between demo screens,
 * with menus for switching theme, dialog icons and the window icon.
 */
public final class DemoApp {

    private DemoApp() {
    }

    private static final String ICONS = "com/dframework/demo/icons/";

    /** The classic DFramework window icons, offered in the Window Icon menu. */
    private static final String[][] WINDOW_ICONS = {
            {"Alien", "alien10.gif"}, {"Blue Alien", "alien15.gif"}, {"Flying Saucer", "alien21.gif"},
            {"Starburst", "alien3.gif"}, {"Pirate Skull", "skull3.gif"}, {"Skull and Crossbones", "skull8.gif"},
            {"Old Bones", "skull7.gif"}, {"Skull", "skull10.gif"}, {"Burger", "food9.gif"},
            {"Cocktail", "food24.gif"}, {"Coffee", "food25.gif"}, {"CD-ROM", "cdrom1.gif"},
            {"Floppy Disk", "COflp16.gif"}, {"Book", "BOOK02.GIF"}, {"Film Reel", "FILM01.GIF"},
            {"Palette", "palette.GIF"}, {"Shades", "shades.GIF"}, {"Company", "company.gif"},
    };

    public static void main(String[] args) {
        ThemeManager.applyOrFallback(DTheme.FLAT_LIGHT, DTheme.SYSTEM);
        // Spinning road signs on every dialog: a bit much, but this is a demo.
        MessageIcons.use(MessageIcons.Style.ANIMATED);
        SwingUtilities.invokeLater(DemoApp::createAndShow);
    }

    private static void createAndShow() {
        JLabel status = new JLabel();
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        String hint = "      Press Ctrl+Shift+P for the command palette";
        ThemeManager.addThemeListener(theme -> status.setText("Theme: " + theme.displayName() + hint));
        ThemeManager.current().ifPresent(theme -> status.setText("Theme: " + theme.displayName() + hint));

        CardLayout cards = new CardLayout();
        JPanel screens = new JPanel(cards);
        DBreadcrumbBar crumbs = new DBreadcrumbBar();
        DOutlookBar nav = navigation(screens, cards, crumbs);
        linkBreadcrumbs(nav, crumbs);

        JPanel main = new JPanel(new BorderLayout());
        main.add(crumbs, BorderLayout.NORTH);
        main.add(screens, BorderLayout.CENTER);
        JPanel root = new JPanel(new BorderLayout());
        root.add(nav, BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        DFrame frame = DFrame.builder()
                .title("DFramework Demo")
                .icon(ICONS + "alien10.gif")
                .closeAction(CloseAction.CONFIRM_EXIT)
                .lafDecorated(true)
                .content(root)
                .size(1000, 640)
                .rememberBounds("dframework.demo.main")
                .build();
        DCommandPalette palette = DCommandPalette.install(frame).addOutlookBar(nav);
        palette.addCommands(windowCommands(frame));
        frame.setJMenuBar(menuBar(frame, nav, palette));
        frame.setVisible(true);
    }

    /** The Outlook bar drives the demo: each item shows a screen in the card panel. */
    /**
     * Keeps the breadcrumb in step with the Outlook bar, and makes clicking a
     * crumb navigate: the root goes to the first screen, a group to its first item.
     */
    private static void linkBreadcrumbs(DOutlookBar nav, DBreadcrumbBar crumbs) {
        Icon home = new ImageIcon(DResources.load(ICONS + "alien10.gif").orElseThrow()
                .getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        Runnable sync = () -> nav.getSelectedItem().ifPresent(item -> crumbs.setPath(
                new Crumb("DFramework", home, nav.getGroups().get(0).getItems().get(0)),
                Crumb.of(item.group().getTitle(), item.group().getItems().get(0)),
                Crumb.of(item.getText().replace('\n', ' '), item)));
        nav.addPropertyChangeListener(DOutlookBar.SELECTED_ITEM_PROPERTY, e -> sync.run());
        sync.run();
        crumbs.addCrumbListener(crumb -> {
            if (crumb.value() instanceof DOutlookBar.Item item) {
                nav.setSelectedItem(item);
            }
        });
    }

    private static DOutlookBar navigation(JPanel screens, CardLayout cards, DBreadcrumbBar crumbs) {
        DOutlookBar bar = new DOutlookBar();

        DOutlookBar.Group showcase = bar.addGroup("Showcase");
        addScreen(showcase, screens, cards, "Controls", "ScreenPos.GIF", ShowcaseScreens.controls());
        addScreen(showcase, screens, cards, "Fields", "check.GIF", FieldsScreen.create());
        addScreen(showcase, screens, cards, "Toasts", "food24.gif", ToastsScreen.create());
        addScreen(showcase, screens, cards, "Table", "BOOK02.GIF", CallsScreen.create("", crumbs));
        addScreen(showcase, screens, cards, "Notes", "Log.gif", ShowcaseScreens.notes());

        // A tribute to the VB6 bar this control is modelled on.
        DOutlookBar.Group tracker = bar.addGroup("Tracker 2");
        addPlaceholder(tracker, screens, cards, "Log Call", "FILM01.GIF");
        addPlaceholder(tracker, screens, cards, "Open Call", "cdrom1.gif");
        addScreen(tracker, screens, cards, "My Calls", "company.gif", CallsScreen.create("engineer:dale -closed", crumbs));
        addPlaceholder(tracker, screens, cards, "Time\nSheet", "food25.gif");

        DOutlookBar.Group admin = bar.addGroup("Admin");
        addPlaceholder(admin, screens, cards, "Users", "chgpwd.GIF");
        addPlaceholder(admin, screens, cards, "Settings", "palette.GIF");
        addPlaceholder(admin, screens, cards, "Backup", "COflp16.gif");
        admin.addItem("About\nDFramework", icon("alien10.gif"), e -> JOptionPane.showMessageDialog(bar,
                "DFramework: Swing, with a better experience out of the box.\nFirst written 2001, revived 2026.",
                "About", JOptionPane.INFORMATION_MESSAGE));

        bar.setSelectedItem(showcase.getItems().get(0));
        return bar;
    }

    private static void addScreen(DOutlookBar.Group group, JPanel screens, CardLayout cards,
            String title, String icon, JComponent screen) {
        String card = group.getTitle() + "/" + title;
        screens.add(screen, card);
        group.addItem(title, icon(icon), e -> cards.show(screens, card));
    }

    private static void addPlaceholder(DOutlookBar.Group group, JPanel screens, CardLayout cards,
            String title, String icon) {
        addScreen(group, screens, cards, title, icon,
                ShowcaseScreens.placeholder(group.getTitle(), title, icon(icon)));
    }

    private static Icon icon(String name) {
        return DResources.load(ICONS + name).orElseThrow();
    }

    /** Extra commands that exist only in the palette. */
    private static List<Command> windowCommands(DFrame frame) {
        return List.of(
                Command.of("Window", "Maximize", frame::maximize),
                Command.of("Window", "Restore", frame::restore),
                Command.of("Window", "Centre on Screen", () -> frame.setLocationRelativeTo(null)),
                Command.of("Window", "Forget Saved Position", () -> {
                    frame.getWindowMemory().ifPresent(WindowMemory::forget);
                    DToast.info(frame, "Next time, the demo opens at its default size and position.");
                }),
                Command.of("Notify", "Say Hello", () -> DToast.success(frame, "Hello from the command palette!")));
    }

    private static JMenuBar menuBar(DFrame frame, DOutlookBar nav, DCommandPalette palette) {
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem commands = new JMenuItem("Command Palette...");
        commands.setAccelerator(DCommandPalette.DEFAULT_KEY_STROKE);
        commands.addActionListener(e -> palette.open());
        file.add(commands);
        file.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.setMnemonic(KeyEvent.VK_X);
        exit.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
        // The same closer as the window's close button, so both ask the same question.
        frame.getCloser().ifPresent(exit::addActionListener);
        file.add(exit);

        JMenu theme = new JMenu("Theme");
        theme.setMnemonic(KeyEvent.VK_T);
        ButtonGroup group = new ButtonGroup();
        for (DTheme each : DTheme.available()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(each.displayName());
            item.setSelected(ThemeManager.current().orElse(null) == each);
            item.addActionListener(e -> ThemeManager.apply(each));
            group.add(item);
            theme.add(item);
        }
        theme.addSeparator();
        JCheckBoxMenuItem classic = new JCheckBoxMenuItem("Classic Outlook Bar (Tracker 2)");
        classic.addActionListener(e -> {
            if (classic.isSelected()) {
                nav.applyClassicStyle();
            } else {
                nav.applyThemeStyle();
            }
        });
        theme.add(classic);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(theme);
        bar.add(dialogsMenu(frame));
        bar.add(windowIconMenu(frame));
        return bar;
    }

    private static JMenu dialogsMenu(DFrame frame) {
        JMenu menu = new JMenu("Dialogs");
        menu.setMnemonic(KeyEvent.VK_D);

        JCheckBoxMenuItem animated = new JCheckBoxMenuItem("Animated Icons",
                MessageIcons.current() == MessageIcons.Style.ANIMATED);
        animated.addActionListener(e -> MessageIcons.use(
                animated.isSelected() ? MessageIcons.Style.ANIMATED : MessageIcons.Style.LOOK_AND_FEEL));
        menu.add(animated);
        menu.addSeparator();

        // Plain JOptionPane calls: MessageIcons styles them without any wrapper.
        menu.add(item("Information...", () -> JOptionPane.showMessageDialog(frame,
                "DFramework was first written in 2001.", "Information", JOptionPane.INFORMATION_MESSAGE)));
        menu.add(item("Warning...", () -> JOptionPane.showMessageDialog(frame,
                "These icons may be a bit much.", "Warning", JOptionPane.WARNING_MESSAGE)));
        menu.add(item("Error...", () -> JOptionPane.showMessageDialog(frame,
                "Something has gone horribly wrong.", "Error", JOptionPane.ERROR_MESSAGE)));
        menu.add(item("Question...", () -> JOptionPane.showConfirmDialog(frame,
                "Do you like the spinning signs?", "Question", JOptionPane.YES_NO_OPTION)));
        return menu;
    }

    private static JMenu windowIconMenu(DFrame frame) {
        JMenu menu = new JMenu("Window Icon");
        menu.setMnemonic(KeyEvent.VK_W);
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem dflt = new JRadioButtonMenuItem("DFramework Default");
        dflt.addActionListener(e -> frame.useDefaultIcon());
        group.add(dflt);
        menu.add(dflt);
        menu.addSeparator();

        for (String[] entry : WINDOW_ICONS) {
            String location = ICONS + entry[1];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(entry[0],
                    DResources.load(location).orElseThrow());
            item.setSelected(entry[1].equals("alien10.gif"));
            item.addActionListener(e -> frame.setIcon(location));
            group.add(item);
            menu.add(item);
        }
        return menu;
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }
}
