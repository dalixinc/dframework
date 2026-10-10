package com.dframework.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.dframework.swing.close.ConfirmCloser;
import com.dframework.swing.dialog.MessageIcons;
import com.dframework.swing.dialog.MessageType;
import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;

/** Creates real (never shown) frames, so needs a display. Skipped when headless. */
class DFrameTest {

    private static final String ALT_ICON = "com/dframework/swing/icons/question.gif";

    private DFrame frame;
    private final List<? extends Image> originalDefaults = DFrame.getDefaultIcons();

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
    }

    @AfterEach
    void dispose() throws Exception {
        DFrame.setDefaultIcons(originalDefaults);
        if (frame != null) {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void defaultsToExitWithAppIcon() throws Exception {
        frame = onEdt(() -> new DFrame("Plain"));

        assertThat(frame.getTitle()).isEqualTo("Plain");
        assertThat(frame.getCloseAction()).isEqualTo(CloseAction.EXIT);
        assertThat(frame.getDefaultCloseOperation()).isEqualTo(WindowConstants.EXIT_ON_CLOSE);
        assertThat(frame.getIconImage()).isNotNull();
        assertThat(frame.getCloser()).isEmpty();
    }

    @Test
    void builderAppliesOptions() throws Exception {
        JLabel content = new JLabel("hello");
        frame = onEdt(() -> DFrame.builder()
                .title("Built")
                .closeAction(CloseAction.DISPOSE)
                .bounds(new Rectangle(10, 20, 300, 200))
                .content(content)
                .build());

        assertThat(frame.getTitle()).isEqualTo("Built");
        assertThat(frame.getDefaultCloseOperation()).isEqualTo(WindowConstants.DISPOSE_ON_CLOSE);
        assertThat(frame.getBounds()).isEqualTo(new Rectangle(10, 20, 300, 200));
        assertThat(content.getParent()).isSameAs(frame.getContentPane());
        assertThat(frame.isVisible()).isFalse();
    }

    @Test
    void confirmActionInstallsCloser() throws Exception {
        frame = onEdt(() -> DFrame.builder().closeAction(CloseAction.CONFIRM_DISPOSE).build());

        assertThat(frame.getDefaultCloseOperation()).isEqualTo(WindowConstants.DO_NOTHING_ON_CLOSE);
        ConfirmCloser closer = frame.getCloser().orElseThrow();
        assertThat(frame.getWindowListeners()).contains(closer);
    }

    @Test
    void changingCloseActionKeepsForeignListeners() throws Exception {
        // Regression: the 2012 DFrame removed *every* window listener here.
        WindowListener mine = new WindowAdapter() { };
        frame = onEdt(() -> {
            DFrame f = DFrame.builder().closeAction(CloseAction.CONFIRM_EXIT).build();
            f.addWindowListener(mine);
            f.setCloseAction(CloseAction.HIDE);
            return f;
        });

        assertThat(frame.getWindowListeners()).contains(mine).noneMatch(l -> l instanceof ConfirmCloser);
        assertThat(frame.getCloser()).isEmpty();
    }

    @Test
    void appWideDefaultIconAppliesToNewFrames() throws Exception {
        DFrame.setDefaultIcon(ALT_ICON);
        frame = onEdt(() -> new DFrame("Custom default"));

        assertThat(frame.getIconImages()).containsExactlyElementsOf(DFrame.getDefaultIcons());
        assertThat(frame.getIconImages()).doesNotContainAnyElementsOf(originalDefaults);
    }

    @Test
    void iconCanBeSetFromLocationOrIconAndReset() throws Exception {
        frame = onEdt(() -> DFrame.builder().icon(ALT_ICON).build());
        Image fromLocation = frame.getIconImage();
        assertThat(frame.getIconImages()).doesNotContainAnyElementsOf(originalDefaults);

        SwingUtilities.invokeAndWait(() -> frame.setIcon(MessageIcons.animated(MessageType.ERROR)));
        assertThat(frame.getIconImage()).isNotSameAs(fromLocation);

        SwingUtilities.invokeAndWait(frame::useDefaultIcon);
        assertThat(frame.getIconImages()).containsExactlyElementsOf(originalDefaults);
    }

    @Test
    void missingIconLocationFailsLoudly() throws Exception {
        frame = onEdt(() -> new DFrame("Missing icon"));

        assertThatThrownBy(() -> frame.setIcon("no/such/icon.gif"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no/such/icon.gif");
    }

    @Test
    void lafDecorationsFollowThemeCapabilities() throws Exception {
        LookAndFeel original = UIManager.getLookAndFeel();
        try {
            ThemeManager.apply(DTheme.FLAT_LIGHT);
            frame = onEdt(() -> DFrame.builder().lafDecorated(true).build());
            assertThat(frame.isUndecorated()).isTrue();
            assertThat(frame.getRootPane().getWindowDecorationStyle()).isEqualTo(JRootPane.FRAME);

            // Nimbus cannot draw title bars, so the OS must take over again.
            ThemeManager.apply(DTheme.NIMBUS);
            SwingUtilities.invokeAndWait(() -> { });
            assertThat(UIManager.getLookAndFeel().getSupportsWindowDecorations()).isFalse();
            assertThat(frame.isUndecorated()).isFalse();
            assertThat(frame.getRootPane().getWindowDecorationStyle()).isEqualTo(JRootPane.NONE);

            ThemeManager.apply(DTheme.FLAT_DARK);
            SwingUtilities.invokeAndWait(() -> { });
            assertThat(frame.isUndecorated()).isTrue();
        } finally {
            UIManager.setLookAndFeel(original);
        }
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }
}
