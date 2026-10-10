package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.PlainDocument;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatClientProperties;

class DTextFieldTest {

    private final DTextField field = new DTextField(20);

    @Test
    void validWithoutValidator() {
        field.setText("anything");

        assertThat(field.isInputValid()).isTrue();
        assertThat(field.getValidationMessage()).isEmpty();
    }

    @Test
    void validatesAsTextChanges() {
        field.setValidator(Validator.required().and(Validator.email()));
        assertThat(field.isInputValid()).isFalse();

        field.setText("dale@example");
        assertThat(field.getValidationMessage()).hasValueSatisfying(m -> assertThat(m).contains("email"));

        field.setText("dale@example.com");
        assertThat(field.isInputValid()).isTrue();
    }

    @Test
    void errorsStayHiddenUntilFieldIsVisited() {
        field.setValidator(Validator.required());
        assertThat(field.isShowingError()).isFalse();
        assertThat(field.getClientProperty(FlatClientProperties.OUTLINE)).isNull();

        loseFocus();

        assertThat(field.isShowingError()).isTrue();
        assertThat(field.getClientProperty(FlatClientProperties.OUTLINE)).isEqualTo(FlatClientProperties.OUTLINE_ERROR);
        assertThat(field.getToolTipText(null)).isEqualTo("This field is required");
    }

    @Test
    void validateInputShowsErrorsImmediately() {
        field.setValidator(Validator.required());

        assertThat(field.validateInput()).isFalse();
        assertThat(field.isShowingError()).isTrue();

        field.resetValidation();
        assertThat(field.isShowingError()).isFalse();
    }

    @Test
    void firesInputValidChanges() {
        List<Object> events = new ArrayList<>();
        field.addPropertyChangeListener(DTextField.INPUT_VALID_PROPERTY, e -> events.add(e.getNewValue()));
        field.setValidator(Validator.integer());

        field.setText("12");
        field.setText("12x");
        field.setText("123");

        assertThat(events).containsExactly(false, true);
    }

    @Test
    void keepsValidatingAfterDocumentIsReplaced() {
        field.setValidator(Validator.required());
        field.setDocument(new PlainDocument());

        field.setText("now valid");
        assertThat(field.isInputValid()).isTrue();
    }

    @Test
    void focusHighlightIsOnByDefaultAndConfigurable() {
        assertThat(field.getFocusBackground()).isEqualTo(FocusHighlighter.getDefaultColor());

        field.setFocusBackground(Color.PINK);
        assertThat(field.getFocusBackground()).isEqualTo(Color.PINK);

        field.setFocusBackground(null);
        assertThat(field.getFocusBackground()).isNull();
    }

    private void loseFocus() {
        for (var l : field.getFocusListeners()) {
            l.focusLost(new FocusEvent(field, FocusEvent.FOCUS_LOST));
        }
    }
}
