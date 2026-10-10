package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.dframework.swing.button.DToggleSwitch;
import com.dframework.swing.table.DTable;
import com.dframework.swing.table.DTablePanel;
import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;

/** The demo's simpler screens. */
final class ShowcaseScreens {

    private ShowcaseScreens() {
    }

    static JComponent controls() {
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

        DToggleSwitch darkMode = new DToggleSwitch("Dark mode",
                ThemeManager.current().map(DTheme::isDark).orElse(false));
        // ActionListener, not ItemListener: only real clicks switch theme, not the sync below.
        darkMode.addActionListener(e -> ThemeManager.apply(darkMode.isSelected() ? DTheme.FLAT_DARK : DTheme.FLAT_LIGHT));
        ThemeManager.addThemeListener(theme -> darkMode.setSelected(theme.isDark()));
        DToggleSwitch locked = new DToggleSwitch("Locked", true);
        locked.setEnabled(false);
        DToggleSwitch green = new DToggleSwitch("Custom colour", true);
        green.setOnColor(new Color(0x27AE60));
        row(form, c, 7, "Switches", flow(darkMode, new DToggleSwitch("Wi-Fi", true),
                new DToggleSwitch("Bluetooth"), green, locked));

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

    /** The searchable call log; {@code filter} pre-fills the search box. */
    static JComponent calls(String filter) {
        DTable table = new DTable(new CallLog(300));
        table.packColumns(320);
        DTablePanel panel = new DTablePanel(table);
        table.setFilterText(filter);
        return panel;
    }

    static JComponent notes() {
        JTextArea text = new JTextArea("The DFramework, first written in 2001 and revived in 2026.\n\nType here...");
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        return new JScrollPane(text);
    }

    /** Stand-in for a screen a real application would provide. */
    static JComponent placeholder(String group, String title, Icon icon) {
        JLabel label = new JLabel("<html><div style='text-align:center'>" + group + " › " + title.replace("\n", " ")
                + "<br><br><small>In Tracker 2 this was a real screen; here it is a placeholder.</small></div></html>",
                icon, SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 1.3f));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
