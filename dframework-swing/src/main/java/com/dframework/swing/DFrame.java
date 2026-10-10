package com.dframework.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.UIManager;

import com.dframework.swing.close.ConfirmCloser;
import com.dframework.swing.util.DResources;

/**
 * A {@link JFrame} with better defaults: an application icon, a configurable
 * (optionally confirmed) close action, simple window-state control, and
 * centring on screen.
 *
 * <p>Use {@link #builder()} for anything beyond a title:
 *
 * <pre>{@code
 * ThemeManager.apply(DTheme.FLAT_LIGHT);
 * SwingUtilities.invokeLater(() -> DFrame.builder()
 *         .title("My App")
 *         .closeAction(CloseAction.CONFIRM_EXIT)
 *         .content(new MyPanel())
 *         .show());
 * }</pre>
 *
 * <p>Like all Swing components, a DFrame must be created and used on the
 * event dispatch thread.
 */
public class DFrame extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final System.Logger LOG = System.getLogger(DFrame.class.getName());

    private CloseAction closeAction;
    private transient ConfirmCloser closer;

    /** An untitled frame with the default icon that exits the application on close. */
    public DFrame() {
        this("");
    }

    /** A titled frame with the default icon that exits the application on close. */
    public DFrame(String title) {
        super(title);
        setIconImage(DResources.appIcon().getImage());
        setCloseAction(CloseAction.EXIT);
    }

    /** Starts building a frame. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Changes what happens when the user closes the window. Only listeners that
     * DFrame installed itself are replaced; your own window listeners are kept.
     */
    public void setCloseAction(CloseAction action) {
        Objects.requireNonNull(action, "action");
        if (closer != null) {
            removeWindowListener(closer);
            closer = null;
        }
        switch (action) {
            case EXIT -> setDefaultCloseOperation(EXIT_ON_CLOSE);
            case DISPOSE -> setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            case HIDE -> setDefaultCloseOperation(HIDE_ON_CLOSE);
            case NOTHING -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            case CONFIRM_EXIT -> installCloser(ConfirmCloser.exitApplication(this));
            case CONFIRM_DISPOSE -> installCloser(ConfirmCloser.disposeWindow(this));
        }
        closeAction = action;
    }

    public CloseAction getCloseAction() {
        return closeAction;
    }

    /**
     * The confirmation closer, present when the close action is
     * {@link CloseAction#CONFIRM_EXIT} or {@link CloseAction#CONFIRM_DISPOSE}.
     * Use it to change the wording or icon, or attach it to an "Exit" menu item
     * so that it asks the same question as the close button.
     */
    public Optional<ConfirmCloser> getCloser() {
        return Optional.ofNullable(closer);
    }

    /**
     * Puts the window into {@code state}. If the platform cannot maximise,
     * {@link WindowState#MAXIMIZED} falls back to filling the usable screen area.
     */
    public void setWindowState(WindowState state) {
        Objects.requireNonNull(state, "state");
        if (state == WindowState.NORMAL || getToolkit().isFrameStateSupported(state.frameState())) {
            setExtendedState(state.frameState());
        } else if (state == WindowState.MAXIMIZED) {
            setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        } else {
            LOG.log(System.Logger.Level.WARNING, "Window state {0} is not supported on this platform", state);
        }
    }

    /** The current window state, read live from the frame. */
    public WindowState getWindowState() {
        return WindowState.fromFrameState(getExtendedState());
    }

    public void maximize() {
        setWindowState(WindowState.MAXIMIZED);
    }

    public void minimize() {
        setWindowState(WindowState.MINIMIZED);
    }

    public void restore() {
        setWindowState(WindowState.NORMAL);
    }

    private void installCloser(ConfirmCloser newCloser) {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        closer = newCloser;
        addWindowListener(closer);
    }

    /** Fluent builder for {@link DFrame}. Unset options keep DFrame's defaults. */
    public static final class Builder {

        private String title = "";
        private List<? extends Image> icons;
        private CloseAction closeAction = CloseAction.EXIT;
        private WindowState windowState = WindowState.NORMAL;
        private Rectangle bounds;
        private Dimension size;
        private Component content;
        private JMenuBar menuBar;
        private boolean lafDecorated;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /** Window icon; defaults to the DFramework application icon. */
        public Builder icon(Image icon) {
            return icons(List.of(icon));
        }

        /** Window icons in several sizes; the platform picks the best fit. */
        public Builder icons(List<? extends Image> icons) {
            this.icons = List.copyOf(icons);
            return this;
        }

        public Builder closeAction(CloseAction closeAction) {
            this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
            return this;
        }

        public Builder windowState(WindowState windowState) {
            this.windowState = Objects.requireNonNull(windowState, "windowState");
            return this;
        }

        /** Exact position and size. Without this, the frame is centred on screen. */
        public Builder bounds(Rectangle bounds) {
            this.bounds = new Rectangle(bounds);
            return this;
        }

        /** Size of the frame. Without this or {@link #bounds}, the frame is packed. */
        public Builder size(int width, int height) {
            this.size = new Dimension(width, height);
            return this;
        }

        /** Component placed in the centre of the content pane. */
        public Builder content(Component content) {
            this.content = content;
            return this;
        }

        public Builder menuBar(JMenuBar menuBar) {
            this.menuBar = menuBar;
            return this;
        }

        /**
         * Draws the title bar and border with the look and feel (for example
         * FlatLaf's themed title bar) instead of the OS window manager. Ignored
         * if the current look and feel cannot decorate windows.
         */
        public Builder lafDecorated(boolean lafDecorated) {
            this.lafDecorated = lafDecorated;
            return this;
        }

        /** Creates the frame without showing it. */
        public DFrame build() {
            DFrame frame = new DFrame(title);
            if (lafDecorated && UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
                frame.setUndecorated(true);
                frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            }
            if (icons != null) {
                frame.setIconImages(icons);
            }
            frame.setCloseAction(closeAction);
            if (menuBar != null) {
                frame.setJMenuBar(menuBar);
            }
            if (content != null) {
                frame.getContentPane().add(content, BorderLayout.CENTER);
            }
            if (bounds != null) {
                frame.setBounds(bounds);
            } else {
                if (size != null) {
                    frame.setSize(size);
                } else {
                    frame.pack();
                }
                frame.setLocationRelativeTo(null);
            }
            frame.setWindowState(windowState);
            return frame;
        }

        /** Creates the frame and makes it visible. */
        public DFrame show() {
            DFrame frame = build();
            frame.setVisible(true);
            return frame;
        }
    }
}
