package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DTextFieldPasteTest {

    @Test
    void pastedLinesAndTableRowsBecomeOneValue() {
        assertThat(DTextField.firstValue("a1fa325\n")).isEqualTo("a1fa325");
        assertThat(DTextField.firstValue("\r\n  main  \r\nmore")).isEqualTo("main");
        assertThat(DTextField.firstValue("a1fa325\tMerge branch")).isEqualTo("a1fa325");
        assertThat(DTextField.firstValue(" spaces kept ")).isEqualTo(" spaces kept ");
    }

    @Test
    void aFilteredFieldAcceptsAPasteWithATrailingNewline() {
        DTextField field = new DTextField();
        field.setInputFilter(InputFilter.regex("[^\\x00-\\x1f]*"));
        field.replaceSelection("HEAD:README.md\n");
        assertThat(field.getText()).isEqualTo("HEAD:README.md");

        field.setPasteCleaner(null);
        field.setText("");
        field.replaceSelection("HEAD\n");
        assertThat(field.getText()).isEmpty();   // refused whole, as before
    }
}
