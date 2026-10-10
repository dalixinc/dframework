package com.dframework.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
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

    private static volatile List<? extends Image> defaultIcons = List.of(DResources.appIcon().getImage());

    private CloseAction closeAction;
    private transient ConfirmCloser closer;
    private boolean lafDecorated;
    private transient WindowMemory memory;

    /** An untitled frame with the {@linkplain #setDefaultIcon default icon} that exits the application on close. */
    public DFrame() {
        this("");
    }

    /** A titled frame with the {@linkplain #setDefaultIcon default icon} that exits the application on close. */
    public DFrame(String title) {
        super(title);
        useDefaultIcon();
        setCloseAction(CloseAction.EXIT);
    }

    /**
     * Sets the icon every new DFrame starts with, application-wide. Existing
     * frames keep theirs until {@link #useDefaultIcon()} is called.
     * Initially the DFramework icon.
     */
    public static void setDefaultIcon(Image icon) {
        setDefaultIcons(List.of(icon));
    }

    /**
     * Sets the default icon from a classpath resource or file path.
     *
     * @throws IllegalArgumentException if there is no image at {@code location}
     * @see #setDefaultIcon(Image)
     */
    public static void setDefaultIcon(String location) {
        setDefaultIcon(loadImage(location));
    }

    /** Sets default icons in several sizes; the platform picks the best fit. */
    public static void setDefaultIcons(List<? extends Image> icons) {
        if (icons.isEmpty()) {
            throw new IllegalArgumentException("At least one icon is required");
        }
        defaultIcons = List.copyOf(icons);
    }

    /** The icons new DFrames start with. */
    public static List<? extends Image> getDefaultIcons() {
        return defaultIcons;
    }

    /** Puts the application-wide {@linkplain #setDefaultIcon default icon} back on this frame. */
    public void useDefaultIcon() {
        setIconImages(defaultIcons);
    }

    /**
     * Sets the window icon from a classpath resource or file path, e.g.
     * {@code "com/myapp/logo.png"} or {@code "./icons/alien10.gif"}.
     *
     * @throws IllegalArgumentException if there is no image at {@code location}
     */
    public void setIcon(String location) {
        setIconImage(loadImage(location));
    }

    /** Sets the window icon from any Swing {@link Icon}. */
    public void setIcon(Icon icon) {
        setIconImage(toImage(icon));
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
     * Draws the title bar and border with the look and feel (for example
     * FlatLaf's themed title bar, which also shows the window icon) instead of
     * the OS window manager.
     *
     * <p>If the theme is later switched to one that cannot decorate windows,
     * such as GTK+, the frame falls back to the OS title bar, and goes back to
     * the themed one when a capable theme returns.
     */
    public void setLafDecorated(boolean lafDecorated) {
        this.lafDecorated = lafDecorated;
        syncDecorations();
    }

    public boolean isLafDecorated() {
        return lafDecorated;
    }

    /**
     * Remembers this frame's position, size and maximised state under
     * {@code id} between runs, and restores what was saved last time. Use an id
     * unique to your application, e.g. {@code "tracker2.main"}.
     *
     * @return whether saved bounds were restored
     * @see WindowMemory
     */
    public boolean rememberBounds(String id) {
        if (memory != null) {
            memory.uninstall();
        }
        memory = WindowMemory.install(this, id);
        return memory.restore();
    }

    /** The window memory, if {@link #rememberBounds(String)} was called. */
    public Optional<WindowMemory> getWindowMemory() {
        return Optional.ofNullable(memory);
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

    @Override
    protected JRootPane createRootPane() {
        JRootPane pane = new JRootPane() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void updateUI() {
                super.updateUI();
                // Runs on every theme change; deferred because the first call
                // happens while JFrame's constructor is still running.
                SwingUtilities.invokeLater(DFrame.this::syncDecorations);
            }
        };
        pane.setOpaque(true);
        return pane;
    }

    /** Matches the decorations to {@link #lafDecorated} and what the current look and feel supports. */
    private void syncDecorations() {
        boolean decorate = lafDecorated && UIManager.getLookAndFeel().getSupportsWindowDecorations();
        if (decorate == (getRootPane().getWindowDecorationStyle() != JRootPane.NONE)) {
            return;
        }
        // Decorations can only change while the frame is not displayable.
        boolean visible = isVisible();
        int state = getExtendedState();
        if (isDisplayable()) {
            dispose();
        }
        setUndecorated(decorate);
        getRootPane().setWindowDecorationStyle(decorate ? JRootPane.FRAME : JRootPane.NONE);
        if (visible) {
            setVisible(true);
            setExtendedState(state);
        }
    }

    private static Image loadImage(String location) {
        return DResources.load(location)
                .orElseThrow(() -> new IllegalArgumentException("No image found at " + location))
                .getImage();
    }

    private static Image toImage(Icon icon) {
        if (icon instanceof ImageIcon imageIcon) {
            return imageIcon.getImage();
        }
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        try {
            icon.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
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
        private String rememberId;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /** Window icon; defaults to {@link DFrame#getDefaultIcons()}. */
        public Builder icon(Image icon) {
            return icons(List.of(icon));
        }

        /** Window icon from any Swing {@link Icon}. */
        public Builder icon(Icon icon) {
            return icon(toImage(icon));
        }

        /**
         * Window icon from a classpath resource or file path.
         *
         * @throws IllegalArgumentException if there is no image at {@code location}
         */
        public Builder icon(String location) {
            return icon(loadImage(location));
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
         * Remembers position, size and maximised state between runs under
         * {@code id}. Saved values take precedence over {@link #bounds},
         * {@link #size} and {@link #windowState}. See {@link DFrame#rememberBounds(String)}.
         */
        public Builder rememberBounds(String id) {
            this.rememberId = Objects.requireNonNull(id, "id");
            return this;
        }

        /** See {@link DFrame#setLafDecorated(boolean)}. */
        public Builder lafDecorated(boolean lafDecorated) {
            this.lafDecorated = lafDecorated;
            return this;
        }

        /** Creates the frame without showing it. */
        public DFrame build() {
            DFrame frame = new DFrame(title);
            frame.setLafDecorated(lafDecorated);
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
            boolean restored = rememberId != null && frame.rememberBounds(rememberId);
            if (!restored) {
                frame.setWindowState(windowState);
            }
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
