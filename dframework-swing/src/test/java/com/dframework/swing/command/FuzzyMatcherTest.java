package com.dframework.swing.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FuzzyMatcherTest {

    @Test
    void requiresCharactersInOrder() {
        assertThat(FuzzyMatcher.match("fdark", "FlatLaf Dark")).isNotNull();
        assertThat(FuzzyMatcher.match("kraD", "FlatLaf Dark")).isNull();
        assertThat(FuzzyMatcher.match("xyz", "FlatLaf Dark")).isNull();
    }

    @Test
    void isCaseInsensitiveAndIgnoresQuerySpaces() {
        assertThat(FuzzyMatcher.match("FLAT DARK", "flatlaf dark")).isNotNull();
    }

    @Test
    void queryCharacterAppearingAtTextStartDoesNotCrash() {
        // Regression: 'a' occurs at index 0, where no previous query character can precede it.
        assertThat(FuzzyMatcher.match("ka", "abc kab")).isNotNull();
        assertThat(FuzzyMatcher.match("aa", "aa")).isNotNull();
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertThat(FuzzyMatcher.match("", "anything").score()).isZero();
    }

    @Test
    void prefersWordStartsOverScatteredLetters() {
        // "fd" should land on F(latLaf) D(ark), not on letters inside words.
        FuzzyMatcher.Match match = FuzzyMatcher.match("fd", "FlatLaf Dark");
        assertThat(match.positions()).containsExactly(0, 8);
    }

    @Test
    void findsTheBestAlignmentNotTheFirst() {
        // Greedy matching would take the first 'd' (in "Dialogs"); the best match is "Dark" as a run.
        FuzzyMatcher.Match match = FuzzyMatcher.match("dark", "Dialogs and FlatLaf Dark");
        assertThat(match.positions()).containsExactly(20, 21, 22, 23);
    }

    @Test
    void ranksTighterMatchesHigher() {
        double exact = FuzzyMatcher.match("dark", "FlatLaf Dark").score();
        double scattered = FuzzyMatcher.match("dark", "Dialogs and Rocket Kit").score();
        assertThat(exact).isGreaterThan(scattered);
    }

    @Test
    void camelCaseHumpsCountAsWordStarts() {
        double hump = FuzzyMatcher.match("ld", "FlatLafDark").score();
        double inner = FuzzyMatcher.match("ld", "Foldable").score();
        assertThat(hump).isGreaterThan(inner);
    }
}
