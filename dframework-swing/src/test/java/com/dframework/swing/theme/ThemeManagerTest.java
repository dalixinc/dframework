package com.dframework.swing.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThemeManagerTest {

    private LookAndFeel original;

    @BeforeEach
    void rememberLookAndFeel() {
        original = UIManager.getLookAndFeel();
    }

    @AfterEach
    void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(original);
    }

    @Test
    void bundledAndJdkThemesAreAvailable() {
        assertThat(DTheme.available()).contains(
                DTheme.FLAT_LIGHT, DTheme.FLAT_DARK, DTheme.FLAT_MAC_DARK,
                DTheme.SYSTEM, DTheme.METAL, DTheme.OCEAN, DTheme.NIMBUS);
    }

    @Test
    void missingThirdPartyThemeIsUnavailable() {
        // Kunststoff is only on the demo's classpath, not the library's.
        assertThat(DTheme.KUNSTSTOFF.isAvailable()).isFalse();
    }

    @Test
    void installsFlatLafTheme() {
        ThemeManager.apply(DTheme.FLAT_DARK);

        assertThat(UIManager.getLookAndFeel().getClass().getName()).isEqualTo(DTheme.FLAT_DARK.className());
        assertThat(ThemeManager.current()).contains(DTheme.FLAT_DARK);
    }

    @Test
    void oceanInstallsMetalWithOceanTheme() {
        ThemeManager.apply(DTheme.OCEAN);

        assertThat(UIManager.getLookAndFeel()).isInstanceOf(MetalLookAndFeel.class);
        assertThat(MetalLookAndFeel.getCurrentTheme()).isInstanceOf(OceanTheme.class);
    }

    @Test
    void notifiesListeners() {
        List<DTheme> seen = new ArrayList<>();
        Consumer<DTheme> listener = seen::add;
        ThemeManager.addThemeListener(listener);
        try {
            ThemeManager.apply(DTheme.FLAT_LIGHT);
            ThemeManager.apply(DTheme.NIMBUS);
        } finally {
            ThemeManager.removeThemeListener(listener);
        }

        assertThat(seen).containsExactly(DTheme.FLAT_LIGHT, DTheme.NIMBUS);
    }

    @Test
    void unavailableThemeThrowsAndKeepsCurrentLookAndFeel() {
        ThemeManager.apply(DTheme.FLAT_LIGHT);

        assertThatThrownBy(() -> ThemeManager.apply(DTheme.KUNSTSTOFF))
                .isInstanceOf(ThemeException.class)
                .hasMessageContaining("Kunststoff");
        assertThat(ThemeManager.current()).contains(DTheme.FLAT_LIGHT);
    }

    @Test
    void fallsBackWhenPreferredIsUnavailable() {
        assertThat(ThemeManager.applyOrFallback(DTheme.KUNSTSTOFF, DTheme.FLAT_LIGHT)).isEqualTo(DTheme.FLAT_LIGHT);
    }
}
