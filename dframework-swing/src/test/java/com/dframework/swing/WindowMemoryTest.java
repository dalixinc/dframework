package com.dframework.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Creates real (never shown) frames, so needs a display. Uses a throwaway preferences node. */
class WindowMemoryTest {

    private final Preferences node = Preferences.userRoot().node("com/dframework/test/" + UUID.randomUUID());
    private JFrame first;
    private JFrame second;

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
    }

    @AfterEach
    void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (first != null) {
                first.dispose();
            }
            if (second != null) {
                second.dispose();
            }
        });
        Preferences parent = node.parent();
        node.removeNode();
        if (parent.childrenNames().length == 0) {
            parent.removeNode();   // don't leave an empty test folder in the user's preferences
        }
    }

    @Test
    void nothingToRestoreFirstTime() throws Exception {
        first = onEdt(JFrame::new);

        assertThat(onEdt(() -> WindowMemory.install(first, node).restore())).isFalse();
    }

    @Test
    void restoresSavedBoundsInANewWindow() throws Exception {
        Rectangle bounds = new Rectangle(120, 90, 640, 420);
        first = onEdt(() -> {
            JFrame f = new JFrame();
            f.setBounds(bounds);
            WindowMemory.install(f, node).save();
            return f;
        });

        second = onEdt(JFrame::new);
        boolean restored = onEdt(() -> WindowMemory.install(second, node).restore());

        assertThat(restored).isTrue();
        assertThat(second.getBounds()).isEqualTo(bounds);
    }

    @Test
    void ignoresBoundsThatAreNoLongerOnAnyScreen() throws Exception {
        node.putInt("x", -50_000);
        node.putInt("y", -50_000);
        node.putInt("width", 600);
        node.putInt("height", 400);
        first = onEdt(JFrame::new);
        Rectangle before = first.getBounds();

        assertThat(onEdt(() -> WindowMemory.install(first, node).restore())).isFalse();
        assertThat(first.getBounds()).isEqualTo(before);
    }

    @Test
    void neverRestoresMinimised() throws Exception {
        node.putInt("x", 100);
        node.putInt("y", 100);
        node.putInt("width", 500);
        node.putInt("height", 300);
        node.putInt("state", Frame.ICONIFIED);
        first = onEdt(JFrame::new);

        onEdt(() -> WindowMemory.install(first, node).restore());

        assertThat(first.getExtendedState() & Frame.ICONIFIED).isZero();
    }

    @Test
    void forgetClearsSavedBounds() throws Exception {
        first = onEdt(() -> {
            JFrame f = new JFrame();
            f.setBounds(100, 100, 500, 300);
            WindowMemory memory = WindowMemory.install(f, node);
            memory.save();
            memory.forget();
            return f;
        });

        second = onEdt(JFrame::new);
        assertThat(onEdt(() -> WindowMemory.install(second, node).restore())).isFalse();
    }

    @Test
    void dframeBuilderUsesRememberedBoundsOverDefaults() throws Exception {
        String id = "test/" + UUID.randomUUID();
        Preferences dframeNode = Preferences.userRoot().node("com/dframework/windows").node(id);
        try {
            dframeNode.putInt("x", 150);
            dframeNode.putInt("y", 110);
            dframeNode.putInt("width", 700);
            dframeNode.putInt("height", 450);
            DFrame frame = onEdt(() -> DFrame.builder().size(300, 200).rememberBounds(id).build());
            first = frame;

            assertThat(frame.getBounds()).isEqualTo(new Rectangle(150, 110, 700, 450));
            assertThat(frame.getWindowMemory()).isPresent();
        } finally {
            Preferences parent = dframeNode.parent();
            dframeNode.removeNode();
            if (parent.childrenNames().length == 0) {
                parent.removeNode();
            }
        }
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }
}
