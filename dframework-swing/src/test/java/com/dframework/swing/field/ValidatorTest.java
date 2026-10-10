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
    void emailRejectsMalformedAddresses() {
        for (String bad : new String[] {"a@b..com", ".a@b.com", "a.@b.com", "a@b.com.", "a@-b.com", "a@b.c", "a b@c.com"}) {
            assertThat(Validator.email().validate(bad)).as(bad).isPresent();
        }
        for (String good : new String[] {"a@b.co", "first.last+tag@sub.example.org", "o'neil@x-y.ie"}) {
            assertThat(Validator.email().validate(good)).as(good).isEmpty();
        }
    }

    @Test
    void integerIsAsciiOnly() {
        assertThat(Validator.integer().validate("+42")).isPresent();
        assertThat(Validator.integer().validate("\u0664\u0662")).isPresent();   // Arabic-Indic 42
        assertThat(Validator.integer().validate("-7")).isEmpty();
    }

    @Test
    void hugeNumberGetsTheRangeMessage() {
        assertThat(Validator.integer(0, 130).validate("99999999999999999999"))
                .contains("Enter a number from 0 to 130");
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
