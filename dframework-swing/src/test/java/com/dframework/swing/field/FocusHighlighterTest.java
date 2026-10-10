package com.dframework.swing.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;

class FocusHighlighterTest {

    private final LookAndFeel original = UIManager.getLookAndFeel();
    private final JTextField field = new JTextField(10);
    private final FocusHighlighter highlighter = new FocusHighlighter(Color.YELLOW);

    @AfterEach
    void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(original);
    }

    @Test
    void highlightsAndRestores() {
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);

        highlighter.highlight(field);
        assertThat(field.getBackground()).isEqualTo(Color.YELLOW);
        assertThat(highlighter.isHighlighting(field)).isTrue();

        highlighter.restore();
        assertThat(field.getBackground()).isEqualTo(Color.WHITE);
        assertThat(highlighter.isHighlighting(field)).isFalse();
    }

    @Test
    void swapsUnreadableTextColour() {
        field.setForeground(Color.WHITE);   // as on a dark theme

        highlighter.highlight(field);
        assertThat(field.getForeground()).isEqualTo(Color.BLACK);

        highlighter.restore();
        assertThat(field.getForeground()).isEqualTo(Color.WHITE);
    }

    @Test
    void autoContrastCanBeTurnedOff() {
        field.setForeground(Color.WHITE);
        highlighter.setAutoContrast(false);

        highlighter.highlight(field);
        assertThat(field.getForeground()).isEqualTo(Color.WHITE);
    }

    @Test
    void skipsReadOnlyAndDisabledFields() {
        field.setEditable(false);
        Color readOnly = field.getBackground();   // the theme may grey it out itself
        highlighter.highlight(field);
        assertThat(field.getBackground()).isEqualTo(readOnly);

        field.setEditable(true);
        field.setEnabled(false);
        Color disabled = field.getBackground();
        highlighter.highlight(field);
        assertThat(field.getBackground()).isEqualTo(disabled);
    }

    @Test
    void colourChangeAppliesImmediately() {
        highlighter.highlight(field);
        highlighter.setColor(Color.CYAN);

        assertThat(field.getBackground()).isEqualTo(Color.CYAN);
    }

    @Test
    void restoreAfterThemeChangeUsesNewThemeColours() {
        ThemeManager.apply(DTheme.FLAT_LIGHT);
        JTextField themed = new JTextField();
        highlighter.highlight(themed);

        ThemeManager.apply(DTheme.FLAT_DARK);
        highlighter.restore();

        assertThat(themed.getBackground()).isInstanceOf(UIResource.class);
        assertThat(themed.getBackground()).isEqualTo(UIManager.getColor("TextField.background"));
    }

    @Test
    void contrastRatio() {
        assertThat(FocusHighlighter.contrast(Color.BLACK, Color.WHITE)).isCloseTo(21.0, within(0.01));
        assertThat(FocusHighlighter.contrast(Color.YELLOW, Color.YELLOW)).isCloseTo(1.0, within(0.01));
    }
}
