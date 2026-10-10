package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.text.PlainDocument;

import org.junit.jupiter.api.Test;

class InputFilterTest {

    private final DTextField field = new DTextField(10);

    @Test
    void builtInFilters() {
        assertThat(InputFilter.digits().accept("0123")).isTrue();
        assertThat(InputFilter.digits().accept("12a")).isFalse();
        assertThat(InputFilter.digits().accept("٤٢")).isFalse();
        assertThat(InputFilter.integer().accept("-")).isTrue();
        assertThat(InputFilter.integer().accept("-12")).isTrue();
        assertThat(InputFilter.integer().accept("1-2")).isFalse();
        assertThat(InputFilter.maxLength(3).accept("abcd")).isFalse();
        assertThat(InputFilter.chars("AB ").accept("A B")).isTrue();
        assertThat(InputFilter.chars("AB ").accept("ABC")).isFalse();
        assertThat(InputFilter.regex("[A-Z]{0,3}").accept("AB")).isTrue();
    }

    @Test
    void blocksTypingThatBreaksTheFilter() throws Exception {
        field.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));

        type("1");
        type("a");
        type("2");
        type("3");
        type("4");

        assertThat(field.getText()).isEqualTo("123");
    }

    @Test
    void rejectsAWholePasteIfAnyOfItIsBad() throws Exception {
        field.setInputFilter(InputFilter.digits());
        type("12");

        field.getDocument().insertString(1, "9x9", null);

        assertThat(field.getText()).isEqualTo("12");
    }

    @Test
    void checksTheResultOfReplacingASelection() {
        field.setInputFilter(InputFilter.maxLength(5));
        field.setText("hello");
        field.select(0, 5);

        field.replaceSelection("world");
        assertThat(field.getText()).isEqualTo("world");

        field.selectAll();
        field.replaceSelection("too long");
        assertThat(field.getText()).isEqualTo("world");
    }

    @Test
    void setTextIsFilteredToo() {
        field.setInputFilter(InputFilter.digits());

        field.setText("42");
        field.setText("forty-two");

        assertThat(field.getText()).isEqualTo("42");
    }

    @Test
    void filterSurvivesDocumentReplacement() throws Exception {
        field.setInputFilter(InputFilter.digits());
        field.setDocument(new PlainDocument());

        type("7x");

        assertThat(field.getText()).isEqualTo("7");
    }

    @Test
    void removingTheFilterAllowsAnything() throws Exception {
        field.setInputFilter(InputFilter.digits());
        field.setInputFilter(null);

        type("abc");

        assertThat(field.getText()).isEqualTo("abc");
    }

    /** Inserts at the end, as typing one keystroke at a time would. */
    private void type(String text) throws Exception {
        for (char c : text.toCharArray()) {
            field.getDocument().insertString(field.getDocument().getLength(), String.valueOf(c), null);
        }
    }
}
