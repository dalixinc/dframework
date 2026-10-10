package com.dframework.swing.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;

class MessageIconsTest {

    private LookAndFeel original;

    @BeforeEach
    void rememberLookAndFeel() {
        original = UIManager.getLookAndFeel();
    }

    @AfterEach
    void restore() throws Exception {
        MessageIcons.use(MessageIcons.Style.LOOK_AND_FEEL);
        UIManager.setLookAndFeel(original);
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void animatedIconsAreBundled(MessageType type) {
        ImageIcon icon = MessageIcons.animated(type);

        assertThat(icon.getIconWidth()).isPositive();
        assertThat(icon.getIconHeight()).isPositive();
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void animatedStyleOverridesLookAndFeelIcons(MessageType type) {
        MessageIcons.use(MessageIcons.Style.ANIMATED);

        assertThat(MessageIcons.current()).isEqualTo(MessageIcons.Style.ANIMATED);
        assertThat(MessageIcons.get(type)).isSameAs(MessageIcons.animated(type));
    }

    @Test
    void animatedStyleSurvivesThemeChange() {
        MessageIcons.use(MessageIcons.Style.ANIMATED);
        ThemeManager.apply(DTheme.FLAT_DARK);

        assertThat(MessageIcons.get(MessageType.QUESTION)).isSameAs(MessageIcons.animated(MessageType.QUESTION));
    }

    @Test
    void lookAndFeelStyleRestoresThemeIcons() {
        ThemeManager.apply(DTheme.FLAT_LIGHT);
        Icon themeIcon = MessageIcons.get(MessageType.ERROR);

        MessageIcons.use(MessageIcons.Style.ANIMATED);
        MessageIcons.use(MessageIcons.Style.LOOK_AND_FEEL);

        assertThat(MessageIcons.get(MessageType.ERROR)).isSameAs(themeIcon);
    }

    @Test
    void singleTypeCanBeOverridden() {
        Icon custom = MessageIcons.animated(MessageType.ERROR);
        MessageIcons.set(MessageType.INFORMATION, custom);

        assertThat(MessageIcons.get(MessageType.INFORMATION)).isSameAs(custom);
        assertThat(MessageIcons.get(MessageType.WARNING)).isNotSameAs(MessageIcons.animated(MessageType.WARNING));
    }
}
