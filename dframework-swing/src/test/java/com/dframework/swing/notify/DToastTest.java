package com.dframework.swing.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Component;
import java.awt.Rectangle;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Uses a bare JRootPane (no window), so runs headless. */
class DToastTest {

    private final JRootPane root = new JRootPane();
    private final JPanel owner = new JPanel();

    @BeforeEach
    void setUp() {
        DToast.setAnimated(false);
        root.getContentPane().add(owner);
        root.setSize(800, 600);
        root.doLayout();
        root.getLayeredPane().setSize(800, 600);
    }

    @AfterEach
    void tearDown() {
        DToast.setAnimated(true);
    }

    @Test
    void appearsInTheBottomRightByDefault() throws Exception {
        DToast toast = onEdt(() -> DToast.info(owner, "Saved"));

        assertThat(toast.isShowing()).isTrue();
        assertThat(layer(toast.getComponent())).isEqualTo(JLayeredPane.POPUP_LAYER);
        Rectangle b = toast.getComponent().getBounds();
        assertThat(b.x + b.width).isEqualTo(800 - 16);
        assertThat(b.y + b.height).isEqualTo(600 - 16);
    }

    @Test
    void newerToastsStackNearestTheCorner() throws Exception {
        DToast first = onEdt(() -> DToast.info(owner, "First"));
        DToast second = onEdt(() -> DToast.info(owner, "Second"));

        assertThat(second.getComponent().getY()).isGreaterThan(first.getComponent().getY());
        assertThat(first.getComponent().getY() + first.getComponent().getHeight())
                .isLessThanOrEqualTo(second.getComponent().getY());
    }

    @Test
    void toastsInACornerShareTheWidestWidth() throws Exception {
        DToast shortOne = onEdt(() -> DToast.info(owner, "Hi"));
        DToast longOne = onEdt(() -> DToast.info(owner, "A considerably longer notification message"));

        assertThat(shortOne.getComponent().getWidth()).isEqualTo(longOne.getComponent().getWidth());
        assertThat(shortOne.getComponent().getX()).isEqualTo(longOne.getComponent().getX());
    }

    @Test
    void topPositionsStackDownwards() throws Exception {
        DToast toast = onEdt(() -> DToast.builder(owner).message("Hi").position(DToast.Position.TOP_CENTER).show());

        Rectangle b = toast.getComponent().getBounds();
        assertThat(b.y).isEqualTo(16);
        assertThat(b.x + b.width / 2).isCloseTo(400, org.assertj.core.data.Offset.offset(1));
    }

    @Test
    void dismissRemovesItAndRestacksTheRest() throws Exception {
        DToast first = onEdt(() -> DToast.info(owner, "First"));
        DToast second = onEdt(() -> DToast.info(owner, "Second"));
        int firstY = first.getComponent().getY();

        SwingUtilities.invokeAndWait(second::dismiss);

        assertThat(second.isShowing()).isFalse();
        assertThat(second.getComponent().getParent()).isNull();
        assertThat(first.getComponent().getY()).isGreaterThan(firstY);
    }

    @Test
    void actionButtonRunsAndCloses() throws Exception {
        AtomicBoolean undone = new AtomicBoolean();
        DToast toast = onEdt(() -> DToast.builder(owner).type(DToast.Type.WARNING)
                .title("Deleted").message("3 calls").action("Undo", () -> undone.set(true)).sticky().show());

        SwingUtilities.invokeAndWait(() -> button(toast, "Undo").doClick());

        assertThat(undone).isTrue();
        assertThat(toast.isShowing()).isFalse();
    }

    @Test
    void closesItselfAfterItsDuration() throws Exception {
        DToast toast = onEdt(() -> DToast.builder(owner).message("Brief").duration(Duration.ofMillis(150)).show());

        Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> { });

        assertThat(toast.isShowing()).isFalse();
    }

    @Test
    void textIsEscapedAndExposedToAccessibility() throws Exception {
        DToast toast = onEdt(() -> DToast.error(owner, "<b>not bold</b> & safe"));

        JLabel text = Arrays.stream(((JPanel) toast.getComponent()).getComponents())
                .filter(c -> c instanceof JLabel l && l.getText() != null && l.getText().startsWith("<html>"))
                .map(JLabel.class::cast).findFirst().orElseThrow();
        assertThat(text.getText()).contains("&lt;b&gt;not bold&lt;/b&gt; &amp; safe");
        assertThat(toast.getComponent().getAccessibleContext().getAccessibleName())
                .isEqualTo("error: <b>not bold</b> & safe");
    }

    @Test
    void ownerMustBeInAWindow() {
        assertThatThrownBy(() -> DToast.info(new JPanel(), "Nowhere")).isInstanceOf(IllegalArgumentException.class);
    }

    private int layer(Component c) {
        return JLayeredPane.getLayer((javax.swing.JComponent) c);
    }

    private static AbstractButton button(DToast toast, String text) {
        return find(toast.getComponent(), text);
    }

    private static AbstractButton find(Component c, String text) {
        if (c instanceof AbstractButton b && text.equals(b.getText())) {
            return b;
        }
        if (c instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                AbstractButton found = find(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }
}
