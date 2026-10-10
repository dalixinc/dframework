package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidatorTest {

    @Test
    void requiredRejectsBlank() {
        assertThat(Validator.required().validate("  ")).contains("This field is required");
        assertThat(Validator.required().validate("x")).isEmpty();
    }

    @Test
    void optionalValidatorsAcceptEmptyText() {
        assertThat(Validator.email().validate("")).isEmpty();
        assertThat(Validator.integer(1, 10).validate("")).isEmpty();
    }

    @Test
    void email() {
        assertThat(Validator.email().validate("dale@example.com")).isEmpty();
        assertThat(Validator.email().validate("dale@example")).isPresent();
        assertThat(Validator.email().validate("not an email")).isPresent();
    }

    @Test
    void integerRange() {
        Validator age = Validator.integer(0, 1000);

        assertThat(age.validate(" 42 ")).isEmpty();
        assertThat(age.validate("forty")).contains("Enter a whole number");
        assertThat(age.validate("1001")).contains("Enter a number from 0 to 1000");
    }

    @Test
    void maxLength() {
        assertThat(Validator.maxLength(3).validate("abcd")).contains("Enter at most 3 characters");
        assertThat(Validator.maxLength(3).validate("abc")).isEmpty();
    }

    @Test
    void andStopsAtFirstFailure() {
        Validator v = Validator.required().and(Validator.email());

        assertThat(v.validate("")).contains("This field is required");
        assertThat(v.validate("nope")).hasValueSatisfying(m -> assertThat(m).contains("email"));
        assertThat(v.validate("a@b.co")).isEmpty();
    }

    @Test
    void customPredicate() {
        Validator upper = Validator.of(t -> t.equals(t.toUpperCase()), "Use capitals");

        assertThat(upper.validate("abc")).contains("Use capitals");
        assertThat(upper.validate("ABC")).isEmpty();
    }
}
