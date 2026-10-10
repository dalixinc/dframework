package com.dframework.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.dframework.swing.field.DTextField;
import com.dframework.swing.field.FocusHighlighter;
import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;
import com.dframework.swing.notify.DToast;

/** Demonstrates {@link DTextField} validation and focus highlighting. */
final class FieldsScreen {

    private record Swatch(String name, Color color) {
    }

    private static final List<Swatch> SWATCHES = List.of(
            new Swatch("Yellow", FocusHighlighter.getDefaultColor()),
            new Swatch("Mint", new Color(0xC8F7DC)),
            new Swatch("Sky", new Color(0xCFE8FF)),
            new Swatch("Pink", new Color(0xFFD6E7)),
            new Swatch("Off", null));

    private FieldsScreen() {
    }

    static JComponent create() {
        // Filters block bad keystrokes; validators judge the finished value.
        DTextField name = new DTextField(24);
        name.setInputFilter(InputFilter.maxLength(40));
        name.setValidator(Validator.required());
        DTextField email = new DTextField(24);
        email.setInputFilter(InputFilter.regex("\\S*").and(InputFilter.maxLength(254)));
        email.setValidator(Validator.required().and(Validator.email()));
        DTextField age = new DTextField(6);
        age.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));
        age.setValidator(Validator.integer(0, 130));
        DTextField postcode = new DTextField(10);
        postcode.setInputFilter(InputFilter.regex("[A-Za-z0-9 ]*").and(InputFilter.maxLength(8)));
        postcode.setValidator(Validator.matches("(?i)[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}",
                "Enter a UK postcode, like SW1A 1AA"));
        List<DTextField> fields = List.of(name, email, age, postcode);

        JTextField plain = new JTextField(24);
        plain.setToolTipText("An ordinary JTextField, highlighted only by the global option below");

        JButton submit = new JButton("Submit");
        Runnable updateSubmit = () -> submit.setEnabled(fields.stream().allMatch(DTextField::isInputValid));
        fields.forEach(f -> f.addPropertyChangeListener(DTextField.INPUT_VALID_PROPERTY, e -> updateSubmit.run()));
        updateSubmit.run();
        submit.addActionListener(e -> {
            if (fields.stream().allMatch(DTextField::validateInput)) {
                DToast.success(submit, "Thanks, " + name.getText().strip() + "! Form submitted.");
            }
        });
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> fields.forEach(f -> {
            f.setText("");
            f.resetValidation();
        }));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Validated fields"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        row(form, c, 0, "Name *", name, "Required; typing stops at 40 characters");
        row(form, c, 1, "Email *", email, "Required; spaces are blocked");
        row(form, c, 2, "Age", age, "Optional; digits only, 0 to 130");
        row(form, c, 3, "Postcode", postcode, "Optional; letters, digits and space only");
        row(form, c, 4, "Plain field", plain, "A standard JTextField, for comparison");
        c.gridy = 5;
        c.gridx = 1;
        form.add(flow(submit, clear), c);

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createTitledBorder("Focus highlight"));
        JCheckBox global = new JCheckBox("Also highlight plain Swing fields across the whole app");
        Color[] chosen = {SWATCHES.get(0).color()};
        Runnable updateGlobal = () -> {
            if (global.isSelected() && chosen[0] != null) {
                FocusHighlighter.enableGlobally(chosen[0]);
            } else {
                FocusHighlighter.disableGlobally();
            }
        };
        global.addActionListener(e -> updateGlobal.run());

        ButtonGroup group = new ButtonGroup();
        JPanel swatches = flow();
        for (Swatch swatch : SWATCHES) {
            JRadioButton radio = new JRadioButton(swatch.name(), swatch == SWATCHES.get(0));
            radio.addActionListener(e -> {
                chosen[0] = swatch.color();
                fields.forEach(f -> f.setFocusBackground(swatch.color()));
                updateGlobal.run();
            });
            group.add(radio);
            swatches.add(radio);
        }
        swatches.setAlignmentX(0);
        global.setAlignmentX(0);
        options.add(swatches);
        options.add(global);

        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.add(form, BorderLayout.NORTH);
        column.add(options, BorderLayout.CENTER);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        wrapper.add(column, BorderLayout.NORTH);
        return wrapper;
    }

    private static void row(JPanel form, GridBagConstraints c, int y, String label, JComponent field, String hint) {
        c.gridy = y;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 2;
        c.weightx = 1;   // the hint column soaks up spare width, keeping the form left-aligned
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setEnabled(false);
        form.add(hintLabel, c);
        c.weightx = 0;
    }

    private static JPanel flow(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }
}
