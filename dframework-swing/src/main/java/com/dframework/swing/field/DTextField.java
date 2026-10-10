package com.dframework.swing.field;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Optional;

import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;

/**
 * A {@link JTextField} with built-in validation and a focus highlight.
 *
 * <ul>
 * <li><b>Validation.</b> Give it a {@link Validator} and it checks the text as
 * the user types. Errors appear only once the user has visited the field (or
 * {@link #validateInput()} is called), as a red outline plus a tooltip giving
 * the message. Listen for {@link #INPUT_VALID_PROPERTY} to enable an OK button.
 * <li><b>Focus highlight.</b> While it has focus, the background changes to
 * {@link FocusHighlighter#getDefaultColor() a pale yellow} by default. Change it
 * with {@link #setFocusBackground(Color)}, or pass {@code null} to turn it off.
 * </ul>
 *
 * <pre>{@code
 * DTextField email = new DTextField(20);
 * email.setValidator(Validator.required().and(Validator.email()));
 * email.addPropertyChangeListener(DTextField.INPUT_VALID_PROPERTY, e -> updateOkButton());
 * }</pre>
 */
public class DTextField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Bound property fired when {@link #isInputValid()} changes. */
    public static final String INPUT_VALID_PROPERTY = "inputValid";

    private static final Color ERROR_COLOR = new Color(0xE0, 0x3E, 0x3E);

    private transient Validator validator;
    private transient FocusHighlighter highlighter;
    private transient DocumentListener documentListener;
    private String validationMessage;
    private boolean inputValid = true;
    private boolean touched;

    public DTextField() {
        this(null, 0);
    }

    public DTextField(int columns) {
        this(null, columns);
    }

    public DTextField(String text, int columns) {
        super(text, columns);
        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                runValidation();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                runValidation();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                runValidation();
            }
        };
        getDocument().addDocumentListener(documentListener);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) {
                    touched = true;
                    runValidation();
                }
            }
        });
        setFocusBackground(FocusHighlighter.getDefaultColor());
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** The validator, if any. */
    public Optional<Validator> getValidator() {
        return Optional.ofNullable(validator);
    }

    /** Sets the validator and re-checks the current text; {@code null} accepts anything. */
    public void setValidator(Validator validator) {
        this.validator = validator;
        runValidation();
    }

    /** Whether the current text passes the validator. Always true without one. */
    public boolean isInputValid() {
        return inputValid;
    }

    /** The validator's message for the current text, if it fails. */
    public Optional<String> getValidationMessage() {
        return Optional.ofNullable(validationMessage);
    }

    /**
     * Checks the text now and shows any error even if the user has not visited
     * the field yet; call it on all fields when the user presses OK.
     *
     * @return whether the text is valid
     */
    public boolean validateInput() {
        touched = true;
        runValidation();
        return inputValid;
    }

    /** Hides any error until the user visits the field again, e.g. after clearing a form. */
    public void resetValidation() {
        touched = false;
        runValidation();
    }

    /** Whether an error is currently being displayed. */
    public boolean isShowingError() {
        return touched && !inputValid;
    }

    /** The background used while the field has focus, or {@code null} if highlighting is off. */
    public Color getFocusBackground() {
        return highlighter == null ? null : highlighter.getColor();
    }

    /** Sets the background used while the field has focus; {@code null} turns highlighting off. */
    public void setFocusBackground(Color color) {
        Color old = getFocusBackground();
        if (color == null) {
            if (highlighter != null) {
                highlighter.uninstall(this);
                highlighter = null;
            }
        } else if (highlighter == null) {
            highlighter = FocusHighlighter.install(this, color);
        } else {
            highlighter.setColor(color);
        }
        firePropertyChange("focusBackground", old, color);
    }

    @Override
    public void setDocument(Document doc) {
        // Called by JTextField's constructor before documentListener exists.
        if (documentListener != null) {
            getDocument().removeDocumentListener(documentListener);
            doc.addDocumentListener(documentListener);
        }
        super.setDocument(doc);
        if (documentListener != null) {
            runValidation();
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return isShowingError() ? validationMessage : super.getToolTipText(event);
    }

    @Override
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
        // FlatLaf draws the error outline itself; other looks get one painted over their border.
        if (isShowingError() && !(UIManager.getLookAndFeel() instanceof FlatLaf)) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ERROR_COLOR);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            } finally {
                g2.dispose();
            }
        }
    }

    private void runValidation() {
        validationMessage = validator == null ? null : validator.validate(getText()).orElse(null);
        boolean old = inputValid;
        inputValid = validationMessage == null;
        putClientProperty(FlatClientProperties.OUTLINE, isShowingError() ? FlatClientProperties.OUTLINE_ERROR : null);
        repaint();
        firePropertyChange(INPUT_VALID_PROPERTY, old, inputValid);
    }
}
