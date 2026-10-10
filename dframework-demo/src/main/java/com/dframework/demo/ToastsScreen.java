package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.dframework.swing.button.DToggleSwitch;
import com.dframework.swing.notify.DToast;

/** Demonstrates {@link DToast}. */
final class ToastsScreen {

    private static final String[][] SAMPLES = {
            {"Saved", "Your changes to call #1042 have been saved."},
            {"Heads up", "The server will restart at 18:00 for maintenance."},
            {"Couldn't connect", "localhost (tracker2sqlb) is not responding. Retrying in 30 seconds."},
            {"New call logged", "Customer Acme Ltd reported a printer fault."},
    };

    private ToastsScreen() {
    }

    static JComponent create() {
        JComboBox<DToast.Position> position = new JComboBox<>(DToast.Position.values());
        position.setSelectedItem(DToast.getDefaultPosition());
        position.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                String name = ((DToast.Position) value).name().replace('_', ' ').toLowerCase();
                return super.getListCellRendererComponent(list,
                        Character.toUpperCase(name.charAt(0)) + name.substring(1), index, selected, focused);
            }
        });
        DToggleSwitch withTitle = new DToggleSwitch("Title", true);
        DToggleSwitch withUndo = new DToggleSwitch("Undo button");
        DToggleSwitch sticky = new DToggleSwitch("Stay until closed");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        DToast.Type[] types = {DToast.Type.SUCCESS, DToast.Type.INFO, DToast.Type.WARNING, DToast.Type.ERROR};
        int[] sample = {0, 3, 1, 2};
        for (int i = 0; i < types.length; i++) {
            DToast.Type type = types[i];
            String[] text = SAMPLES[sample[i]];
            String label = type.name().charAt(0) + type.name().substring(1).toLowerCase();
            JButton button = new JButton(label);
            button.addActionListener(e -> {
                DToast.Builder toast = DToast.builder(button)
                        .type(type)
                        .message(text[1])
                        .position((DToast.Position) position.getSelectedItem());
                if (withTitle.isSelected()) {
                    toast.title(text[0]);
                }
                if (withUndo.isSelected()) {
                    toast.action("Undo", () -> DToast.info(button, "Undone."));
                }
                if (sticky.isSelected()) {
                    toast.sticky();
                }
                toast.show();
            });
            buttons.add(button);
        }
        JButton burst = new JButton("Burst of 5");
        burst.addActionListener(e -> {
            for (int i = 1; i <= 5; i++) {
                DToast.builder(burst)
                        .type(DToast.Type.values()[i % 4])
                        .message("Notification " + i + " of 5")
                        .duration(Duration.ofSeconds(2 + i))
                        .position((DToast.Position) position.getSelectedItem())
                        .show();
            }
        });
        buttons.add(burst);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Toast notifications"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        add(form, c, 0, "Show", buttons);
        add(form, c, 1, "Position", position);
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 0));
        options.add(withTitle);
        options.add(withUndo);
        options.add(sticky);
        add(form, c, 2, "Options", options);
        c.gridy = 3;
        c.gridx = 1;
        JLabel hint = new JLabel("Hover over a toast to pause it; click it to close.");
        hint.setEnabled(false);
        form.add(hint, c);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private static void add(JPanel form, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
    }
}
