package com.dframework.fx;

import static com.dframework.fx.FxToolkit.onFx;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.dframework.fx.theme.FxTheme;
import com.dframework.fx.theme.FxThemeManager;
import com.dframework.swing.CloseAction;

class DStageTest {

    @BeforeAll
    static void startJavaFx() {
        FxToolkit.start();
    }

    @Test
    void boundsAreSavedAndRestoredWhenStillOnAScreen() throws Exception {
        String key = "test-" + UUID.randomUUID();
        Preferences node = Preferences.userRoot().node("com/dframework/fx/windows/" + key);
        try {
            onFx(() -> {
                DStage first = new DStage();
                first.rememberBounds(key);
                first.setX(120);
                first.setY(80);
                first.setWidth(640);
                first.setHeight(420);
                first.saveBounds();

                DStage second = new DStage();
                second.rememberBounds(key);
                assertThat(second.getWidth()).isEqualTo(640);
                assertThat(second.getHeight()).isEqualTo(420);
                assertThat(second.getX()).isEqualTo(120);
                assertThat(second.getY()).isEqualTo(80);
            });
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theCloseActionDefaultsToExitAndCanBeChanged() throws Exception {
        onFx(() -> {
            DStage stage = new DStage();
            assertThat(stage.getCloseAction()).isEqualTo(CloseAction.EXIT);
            stage.setCloseAction(CloseAction.CONFIRM_DISPOSE);
            assertThat(stage.getCloseAction()).isEqualTo(CloseAction.CONFIRM_DISPOSE);
        });
    }

    @Test
    void themesSwitchLiveAndTellListeners() throws Exception {
        FxTheme[] seen = new FxTheme[1];
        onFx(() -> {
            FxThemeManager.addThemeListener(t -> seen[0] = t);
            FxThemeManager.apply(FxTheme.NORD_DARK);
        });
        assertThat(FxThemeManager.current()).contains(FxTheme.NORD_DARK);
        assertThat(seen[0]).isEqualTo(FxTheme.NORD_DARK);
        assertThat(FxTheme.NORD_DARK.isDark()).isTrue();
        assertThat(FxTheme.MODENA.isAtlantaFx()).isFalse();
    }
}
