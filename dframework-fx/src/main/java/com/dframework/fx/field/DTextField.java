package com.dframework.fx.field;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;

import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

/**
 * The JavaFX DTextField: the same input filtering, validation, paste tidying
 * and focus highlight as the Swing one, built the JavaFX way.
 * <ul>
 * <li><b>Filtering</b> uses a {@link TextFormatter}, JavaFX's own hook for
 * vetoing edits, so it covers typing, pasting and {@code setText} alike.
 * <li><b>Validity</b> is an observable property, so an OK button can be bound
 * to it: {@code ok.disableProperty().bind(field.validProperty().not())}.
 * <li><b>Errors</b> are a CSS state ({@code :danger}, which AtlantaFX themes
 * draw as a red outline), shown once the user has left the field.
 * </ul>
 * It uses the toolkit-neutral {@link Validator} and {@link InputFilter}, the
 * same classes as the Swing field.
 */
public class DTextField extends TextField {

    /** AtlantaFX's error state; DFramework's stylesheet draws it for other themes. */
    public static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");

    private static final String STYLESHEET = Objects.requireNonNull(
            DTextField.class.getResource("/com/dframework/fx/dframework-fx.css"), "dframework-fx.css").toExternalForm();

    private final ReadOnlyBooleanWrapper valid = new ReadOnlyBooleanWrapper(this, "valid", true);
    private final ReadOnlyStringWrapper validationMessage = new ReadOnlyStringWrapper(this, "validationMessage");
    private final ObjectProperty<Color> focusBackground = new SimpleObjectProperty<>(this, "focusBackground",
            Color.web("#FFF9C4"));
    private final Tooltip errorTip = new Tooltip();
    private Validator validator;
    private InputFilter inputFilter;
    private UnaryOperator<String> pasteCleaner = DTextField::firstValue;
    private boolean touched;

    public DTextField() {
        this("");
    }

    public DTextField(String text) {
        super(text);
        getStyleClass().add("d-text-field");
        getStylesheets().add(STYLESHEET);
        setTextFormatter(new TextFormatter<>(this::filter));
        textProperty().addListener((o, was, now) -> validate());
        focusedProperty().addListener((o, was, focused) -> {
            if (!focused) {
                touched = true;
                validate();
            }
            applyFocusStyle(focused);
        });
        focusBackground.addListener((o, was, now) -> applyFocusStyle(isFocused()));
        validate();
    }

    // ---- filtering ----------------------------------------------------------------------------

    private TextFormatter.Change filter(TextFormatter.Change change) {
        if (pasteCleaner != null && change.isContentChange() && change.getText().length() > 1) {
            String cleaned = Objects.requireNonNullElse(pasteCleaner.apply(change.getText()), "");
            if (!cleaned.equals(change.getText())) {
                change.setText(cleaned);
                change.selectRange(change.getRangeStart() + cleaned.length(), change.getRangeStart() + cleaned.length());
            }
        }
        if (inputFilter != null && change.isContentChange() && !inputFilter.accept(change.getControlNewText())) {
            return null;   // vetoed: the text stays as it was
        }
        return change;
    }

    public Optional<InputFilter> getInputFilter() {
        return Optional.ofNullable(inputFilter);
    }

    /** What may be typed or pasted; {@code null} allows anything. Text already present is left alone. */
    public void setInputFilter(InputFilter inputFilter) {
        this.inputFilter = inputFilter;
    }

    /** How pasted text is tidied before the filter sees it; {@code null} inserts it as it comes. */
    public void setPasteCleaner(UnaryOperator<String> pasteCleaner) {
        this.pasteCleaner = pasteCleaner;
    }

    /**
     * The default paste cleaner, as in the Swing field: text with line breaks or
     * tabs becomes its first non-blank line, then that line's first non-blank
     * tab-separated cell, trimmed. Other text is left alone.
     */
    public static String firstValue(String text) {
        if (text == null || text.indexOf('\n') < 0 && text.indexOf('\r') < 0 && text.indexOf('\t') < 0) {
            return text;
        }
        String line = text.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        for (String cell : line.split("\t")) {
            if (!cell.isBlank()) {
                return cell.strip();
            }
        }
        return "";
    }

    // ---- validation ---------------------------------------------------------------------------

    public Optional<Validator> getValidator() {
        return Optional.ofNullable(validator);
    }

    /** Sets the validator and re-checks the current text; {@code null} accepts anything. */
    public void setValidator(Validator validator) {
        this.validator = validator;
        validate();
    }

    /** Whether the current text passes the validator. Bind buttons to it. */
    public ReadOnlyBooleanProperty validProperty() {
        return valid.getReadOnlyProperty();
    }

    public boolean isValid() {
        return valid.get();
    }

    /** The validator's message for the current text, or {@code null} when valid. */
    public ReadOnlyStringProperty validationMessageProperty() {
        return validationMessage.getReadOnlyProperty();
    }

    /** Checks the field now and shows any error, even if the user hasn't visited it. */
    public boolean validateInput() {
        touched = true;
        validate();
        return isValid();
    }

    /** Forgets that the user has visited the field, hiding any error until they leave it again. */
    public void resetValidation() {
        touched = false;
        validate();
    }

    private void validate() {
        String message = validator == null ? null : validator.validate(getText() == null ? "" : getText()).orElse(null);
        validationMessage.set(message);
        valid.set(message == null);
        boolean showError = touched && message != null;
        pseudoClassStateChanged(DANGER, showError);
        if (showError) {
            errorTip.setText(message);
            setTooltip(errorTip);
        } else if (getTooltip() == errorTip) {
            setTooltip(null);
        }
    }

    // ---- focus highlight ----------------------------------------------------------------------

    /** The background while focused (pale yellow by default, as in 2007); {@code null} turns it off. */
    public ObjectProperty<Color> focusBackgroundProperty() {
        return focusBackground;
    }

    public void setFocusBackground(Color color) {
        focusBackground.set(color);
    }

    private void applyFocusStyle(boolean focused) {
        Color bg = focusBackground.get();
        if (!focused || bg == null) {
            setStyle("");
            return;
        }
        // Black or white text, whichever reads better on the highlight.
        Color text = bg.getBrightness() > 0.6 && bg.getSaturation() < 0.9 ? Color.BLACK : Color.WHITE;
        String b = web(bg);
        String t = web(text);
        // AtlantaFX themes read -color-*; Modena and Caspian read -fx-control-inner-background.
        setStyle("-color-bg-default: " + b + "; -fx-control-inner-background: " + b + ";"
                + " -color-fg-default: " + t + "; -fx-text-fill: " + t + ";");
    }

    private static String web(Color c) {
        return String.format("#%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }
}
