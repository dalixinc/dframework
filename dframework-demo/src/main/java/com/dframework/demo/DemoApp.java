package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.dframework.swing.CloseAction;
import com.dframework.swing.DFrame;
import com.dframework.swing.dialog.MessageIcons;
import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;
import com.dframework.swing.util.DResources;

/**
 * Showcase for DFramework: a frame with a theme switcher and a spread of
 * standard components, so each look and feel can be compared at a glance.
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
        ThemeManager.addThemeListener(theme -> status.setText("Theme: " + theme.displayName()));
        ThemeManager.current().ifPresent(theme -> status.setText("Theme: " + theme.displayName()));

        JPanel root = new JPanel(new BorderLayout());
        root.add(showcase(), BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        DFrame frame = DFrame.builder()
                .title("DFramework Demo")
                .icon(ICONS + "alien10.gif")
                .closeAction(CloseAction.CONFIRM_EXIT)
                .lafDecorated(true)
                .content(root)
                .size(820, 560)
                .build();
        frame.setJMenuBar(menuBar(frame));
        frame.setVisible(true);
    }

    private static JMenuBar menuBar(DFrame frame) {
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
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

    private static JComponent showcase() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Controls", controls());
        tabs.addTab("Table", new JScrollPane(table()));
        tabs.addTab("Text", new JScrollPane(new JTextArea(
                "The DFramework, first written in 2001 and revived in 2026.\n\nType here...")));
        return tabs;
    }

    private static JComponent controls() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("The Border"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        row(form, c, 0, "Name", new JTextField(24));
        row(form, c, 1, "Choice", new JComboBox<>(new String[] {"Metal", "Ocean", "Kunststoff", "FlatLaf"}));
        row(form, c, 2, "Count", new JSpinner());
        row(form, c, 3, "Volume", new JSlider(0, 100, 65));
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(40);
        progress.setStringPainted(true);
        row(form, c, 4, "Progress", progress);

        JRadioButton one = new JRadioButton("One", true);
        JRadioButton two = new JRadioButton("Two");
        ButtonGroup group = new ButtonGroup();
        group.add(one);
        group.add(two);
        row(form, c, 5, "Options", flow(one, two, new JCheckBox("Check me", true)));

        JButton disabled = new JButton("Disabled");
        disabled.setEnabled(false);
        row(form, c, 6, "Buttons", flow(new JButton("Testing"), new JToggleButton("Toggle"), disabled));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    /** Lays components out left to right with a small gap between them. */
    private static JPanel flow(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        for (JComponent component : components) {
            if (panel.getComponentCount() > 0) {
                panel.add(Box.createHorizontalStrut(8));
            }
            panel.add(component);
        }
        return panel;
    }

    private static void row(JPanel form, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
    }

    private static JTable table() {
        String[] columns = {"Component", "Since", "Status"};
        Object[][] rows = {
                {"DFrame", 2001, "Modernised"},
                {"ConfirmCloser", 2026, "New (was QCloser + DCloser)"},
                {"ThemeManager", 2026, "New (was DLookAndFeelManager)"},
                {"DTheme", 2026, "New (was KnownLookAndFeels)"},
        };
        JTable table = new JTable(rows, columns);
        table.setAutoCreateRowSorter(true);
        return table;
    }
}
