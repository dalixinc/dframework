package com.dframework.swing;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Timer;

/**
 * Remembers a window's position, size and maximised state between runs, using
 * {@link Preferences}.
 *
 * <pre>{@code
 * WindowMemory.install(dialog, "tracker2.callDialog").restore();
 * }</pre>
 *
 * <p>For a {@link DFrame}, use {@link DFrame#rememberBounds(String)} or the
 * builder's {@code rememberBounds(id)} instead.
 *
 * <p>Saved bounds are only restored if a reasonable part of them is still on
 * a connected screen, so a window last used on an unplugged monitor comes back
 * where it can be seen. While a frame is maximised, its normal (restored)
 * bounds are what is remembered, so un-maximising next time works as expected.
 */
public final class WindowMemory {

    private static final System.Logger LOG = System.getLogger(WindowMemory.class.getName());

    private static final String X = "x";
    private static final String Y = "y";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String STATE = "state";
    private static final int MIN_VISIBLE_WIDTH = 120;
    private static final int MIN_VISIBLE_HEIGHT = 60;
    private static final int SAVE_DELAY_MS = 400;

    private final Window window;
    private final Preferences node;
    private final Timer saveLater;
    private final ComponentListener moves;
    private final WindowAdapter closes;
    private Rectangle normalBounds;

    private WindowMemory(Window window, Preferences node) {
        this.window = Objects.requireNonNull(window, "window");
        this.node = Objects.requireNonNull(node, "node");
        saveLater = new Timer(SAVE_DELAY_MS, e -> save());
        saveLater.setRepeats(false);
        moves = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                changed();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                changed();
            }
        };
        closes = new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                saveLater.restart();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveNow();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                saveNow();
            }
        };
    }

    /**
     * Starts remembering {@code window} under {@code id}. Use an id unique to
     * your application, e.g. {@code "tracker2.main"}. Call {@link #restore()}
     * to apply what was saved last time.
     */
    public static WindowMemory install(Window window, String id) {
        return install(window, Preferences.userRoot().node("com/dframework/windows").node(id));
    }

    /** Starts remembering {@code window} in the given preferences node. */
    public static WindowMemory install(Window window, Preferences node) {
        WindowMemory memory = new WindowMemory(window, node);
        memory.normalBounds = window.getBounds();
        window.addComponentListener(memory.moves);
        window.addWindowListener(memory.closes);
        window.addWindowStateListener(memory.closes);
        return memory;
    }

    /**
     * Applies the saved bounds and state, if there are any and they are still
     * on screen.
     *
     * @return whether anything was restored
     */
    public boolean restore() {
        if (node.getInt(WIDTH, 0) <= 0 || node.getInt(HEIGHT, 0) <= 0) {
            return false;
        }
        Rectangle saved = new Rectangle(node.getInt(X, 0), node.getInt(Y, 0), node.getInt(WIDTH, 0), node.getInt(HEIGHT, 0));
        if (!isVisibleOnSomeScreen(saved)) {
            LOG.log(System.Logger.Level.INFO, "Saved bounds {0} are off screen; not restoring", saved);
            return false;
        }
        window.setBounds(saved);
        normalBounds = saved;
        if (window instanceof Frame frame) {
            int state = node.getInt(STATE, Frame.NORMAL) & ~Frame.ICONIFIED;   // never start minimised
            if (state != Frame.NORMAL && frame.getToolkit().isFrameStateSupported(state)) {
                frame.setExtendedState(state);
            }
        }
        return true;
    }

    /** Saves the current bounds and state now. Normally happens automatically. */
    public void save() {
        int state = window instanceof Frame frame ? frame.getExtendedState() : Frame.NORMAL;
        Rectangle bounds = (state & Frame.MAXIMIZED_BOTH) != 0 && normalBounds != null ? normalBounds : window.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }
        node.putInt(X, bounds.x);
        node.putInt(Y, bounds.y);
        node.putInt(WIDTH, bounds.width);
        node.putInt(HEIGHT, bounds.height);
        node.putInt(STATE, state & ~Frame.ICONIFIED);
    }

    /** Deletes what was saved, so the next run uses the default bounds. */
    public void forget() {
        saveLater.stop();
        try {
            node.clear();
        } catch (BackingStoreException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not clear saved window bounds", e);
        }
    }

    /** Stops remembering; what was saved so far is kept. */
    public void uninstall() {
        saveLater.stop();
        window.removeComponentListener(moves);
        window.removeWindowListener(closes);
        window.removeWindowStateListener(closes);
    }

    private void changed() {
        boolean normal = !(window instanceof Frame frame) || (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0;
        if (normal) {
            normalBounds = window.getBounds();
        }
        saveLater.restart();
    }

    private void saveNow() {
        saveLater.stop();
        save();
        try {
            node.flush();
        } catch (BackingStoreException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not save window bounds", e);
        }
    }

    private static boolean isVisibleOnSomeScreen(Rectangle bounds) {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle visible = device.getDefaultConfiguration().getBounds().intersection(bounds);
            if (visible.width >= MIN_VISIBLE_WIDTH && visible.height >= MIN_VISIBLE_HEIGHT) {
                return true;
            }
        }
        return false;
    }
}
