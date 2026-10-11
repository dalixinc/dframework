package com.dframework.fx;

import static com.dframework.fx.FxToolkit.onFx;
import static org.assertj.core.api.Assertions.assertThat;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.dframework.fx.field.DTextField;
import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

class DTextFieldTest {

    @BeforeAll
    static void startJavaFx() {
        FxToolkit.start();
    }

    @Test
    void theInputFilterVetoesTypingAndPastingAlike() throws Exception {
        onFx(() -> {
            DTextField age = new DTextField();
            age.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));
            age.replaceSelection("4");
            age.replaceSelection("x");          // a typed letter
            age.replaceSelection("2a");         // a paste with a letter: refused whole
            age.replaceSelection("2");
            age.replaceSelection("99");         // would make four digits
            assertThat(age.getText()).isEqualTo("42");
        });
    }

    @Test
    void aPasteWithANewlineIsTidiedBeforeTheFilterSeesIt() throws Exception {
        onFx(() -> {
            DTextField id = new DTextField();
            id.setInputFilter(InputFilter.regex("[0-9a-f]*"));
            id.replaceSelection("a1fa325\n");
            assertThat(id.getText()).isEqualTo("a1fa325");

            id.setPasteCleaner(null);
            id.clear();
            id.replaceSelection("a1fa325\n");
            assertThat(id.getText()).isEmpty();
        });
    }

    @Test
    void validityIsAPropertyAButtonCanBeBoundTo() throws Exception {
        onFx(() -> {
            DTextField name = new DTextField();
            name.setValidator(Validator.required());
            DTextField email = new DTextField();
            email.setValidator(Validator.email());
            Button ok = new Button("OK");
            BooleanBinding valid = name.validProperty().and(email.validProperty());
            ok.disableProperty().bind(valid.not());

            assertThat(ok.isDisabled()).isTrue();
            name.setText("Dale");
            assertThat(ok.isDisabled()).isFalse();   // an empty email is allowed
            email.setText("not-an-email");
            assertThat(ok.isDisabled()).isTrue();
            assertThat(email.validationMessageProperty().get()).contains("valid email");
        });
    }

    @Test
    void errorsShowOnlyOnceTheFieldHasBeenVisited() throws Exception {
        onFx(() -> {
            DTextField name = new DTextField();
            name.setValidator(Validator.required());
            assertThat(name.getPseudoClassStates()).doesNotContain(DTextField.DANGER);
            assertThat(name.validateInput()).isFalse();
            assertThat(name.getPseudoClassStates()).contains(DTextField.DANGER);
            assertThat(name.getTooltip().getText()).isNotBlank();
            name.setText("Dale");
            assertThat(name.getPseudoClassStates()).doesNotContain(DTextField.DANGER);
            assertThat(name.getTooltip()).isNull();
        });
    }

    @Test
    void firstValueMatchesTheSwingField() {
        for (String text : new String[] {"a1fa325\n", "\r\n  main  \r\nmore", "a1fa325\tMerge", " spaces kept "}) {
            assertThat(DTextField.firstValue(text)).isEqualTo(com.dframework.swing.field.DTextField.firstValue(text));
        }
    }
}
