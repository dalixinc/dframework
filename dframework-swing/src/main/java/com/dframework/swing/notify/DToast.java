package com.dframework.swing.notify;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * A short, non-blocking notification that slides into a corner of the window
 * and fades away by itself.
 *
 * <pre>{@code
 * DToast.success(frame, "Saved");
 * DToast.builder(frame)
 *         .type(DToast.Type.WARNING)
 *         .title("Deleted 3 calls")
 *         .message("They are in the bin for 30 days.")
 *         .action("Undo", this::undoDelete)
 *         .show();
 * }</pre>
 *
 * <p>Toasts stack when several are shown, pause while the mouse is over them,
 * and close on click. They are drawn inside the owner's window, so they move
 * with it.
 */
public final class DToast {

    /** Sets the icon and accent colour. */
    public enum Type {
        INFO(new Color(0x2F80ED)),
        SUCCESS(new Color(0x27AE60)),
        WARNING(new Color(0xE2A100)),
        ERROR(new Color(0xE0443E));

        private final Color accent;

        Type(Color accent) {
            this.accent = accent;
        }

        public Color accent() {
            return accent;
        }
    }

    /** Where in the window toasts appear. */
    public enum Position {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

        boolean isTop() {
            return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
        }
    }

    private static final int MARGIN = 16;
    private static final int GAP = 8;
    private static final int MAX_TEXT_WIDTH = 300;
    private static final int FADE_MS = 180;
    private static final int SLIDE_PX = 14;
    private static final int ANIMATION_TICK_MS = 16;
    private static final int IDLE_TICK_MS = 100;

    private static volatile Position defaultPosition = Position.BOTTOM_RIGHT;
    private static volatile Duration defaultDuration = Duration.ofSeconds(4);
    private static volatile boolean animated = true;

    private final ToastPanel panel;
    private final Stack stack;
    private final Position position;
    private final Duration duration;
    private final Timer timer;
    private long remainingNanos;
    private long lastTick;
    private float opacity;
    private boolean closing;
    private boolean shown;
    /** Whether the final, fully-opaque layout has been done since the last fade. */
    private boolean settled;

    private DToast(Builder b, Stack stack) {
        this.stack = stack;
        this.position = b.position;
        this.duration = b.duration;
        this.panel = new ToastPanel(this, b);
        this.remainingNanos = duration == null ? Long.MAX_VALUE : duration.toNanos();
        this.timer = new Timer(ANIMATION_TICK_MS, e -> tick());
    }

    /** Shows an information toast. */
    public static DToast info(Component owner, String message) {
        return builder(owner).type(Type.INFO).message(message).show();
    }

    /** Shows a success toast. */
    public static DToast success(Component owner, String message) {
        return builder(owner).type(Type.SUCCESS).message(message).show();
    }

    /** Shows a warning toast. */
    public static DToast warning(Component owner, String message) {
        return builder(owner).type(Type.WARNING).message(message).show();
    }

    /** Shows an error toast. */
    public static DToast error(Component owner, String message) {
        return builder(owner).type(Type.ERROR).message(message).show();
    }

    /** Starts building a toast to show in {@code owner}'s window. */
    public static Builder builder(Component owner) {
        return new Builder(owner);
    }

    /** Where toasts appear unless a builder says otherwise. Initially bottom right. */
    public static void setDefaultPosition(Position position) {
        defaultPosition = Objects.requireNonNull(position, "position");
    }

    public static Position getDefaultPosition() {
        return defaultPosition;
    }

    /** How long toasts stay unless a builder says otherwise. Initially 4 seconds. */
    public static void setDefaultDuration(Duration duration) {
        defaultDuration = Objects.requireNonNull(duration, "duration");
    }

    public static Duration getDefaultDuration() {
        return defaultDuration;
    }

    /** Whether toasts fade and slide; turn off for reduced-motion preferences. Default on. */
    public static void setAnimated(boolean animated) {
        DToast.animated = animated;
    }

    public static boolean isAnimated() {
        return animated;
    }

    /** Whether this toast is still on screen (including while fading out). */
    public boolean isShowing() {
        return shown;
    }

    /** Closes the toast now, fading out if animations are on. */
    public void dismiss() {
        onEdt(() -> {
            if (!shown || closing) {
                return;
            }
            closing = true;
            if (!animated) {
                remove();
            } else {
                lastTick = System.nanoTime();
                timer.setDelay(ANIMATION_TICK_MS);
                timer.start();
            }
        });
    }

    /** The toast's component, e.g. for tests or to adjust its accessibility details. */
    public JComponent getComponent() {
        return panel;
    }

    private void open() {
        shown = true;
        opacity = animated ? 0f : 1f;
        stack.add(this);
        lastTick = System.nanoTime();
        if (animated || duration != null) {
            timer.start();
        }
    }

    private void tick() {
        long now = System.nanoTime();
        long elapsed = now - lastTick;
        lastTick = now;
        float step = elapsed / (FADE_MS * 1_000_000f);
        if (closing) {
            opacity = Math.max(0f, opacity - step);
            if (opacity <= 0f) {
                remove();
                return;
            }
        } else {
            if (opacity < 1f) {
                opacity = Math.min(1f, opacity + step);
            } else if (duration == null) {
                timer.stop();   // fully shown and sticky: nothing left to do until dismissed
            }
            if (!isPointerOver()) {
                remainingNanos -= elapsed;
            }
            if (remainingNanos <= 0) {
                dismiss();
            }
        }
        boolean fading = closing || opacity < 1f;
        // Fast ticks only while fading; otherwise just count down.
        timer.setDelay(fading ? ANIMATION_TICK_MS : IDLE_TICK_MS);
        if (fading || !settled) {
            stack.layout();
            panel.repaint();
        }
        settled = !fading;
    }

    private void remove() {
        timer.stop();
        shown = false;
        stack.remove(this);
    }

    private boolean isPointerOver() {
        if (!panel.isShowing()) {
            return false;
        }
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null) {
            return false;
        }
        Point p = pointer.getLocation();
        SwingUtilities.convertPointFromScreen(p, panel);
        return panel.contains(p);
    }

    private static void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /** Configures and shows a {@link DToast}. */
    public static final class Builder {

        private final Component owner;
        private Type type = Type.INFO;
        private String title;
        private String message = "";
        private Duration duration = defaultDuration;
        private Position position = defaultPosition;
        private String actionText;
        private Runnable action;

        private Builder(Component owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        public Builder type(Type type) {
            this.type = Objects.requireNonNull(type, "type");
            return this;
        }

        /** Optional bold first line. */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = Objects.requireNonNull(message, "message");
            return this;
        }

        /** How long before it fades; {@code null} keeps it until closed. */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** Keeps the toast until the user closes it. */
        public Builder sticky() {
            return duration(null);
        }

        public Builder position(Position position) {
            this.position = Objects.requireNonNull(position, "position");
            return this;
        }

        /** Adds a button such as "Undo"; clicking it runs {@code action} and closes the toast. */
        public Builder action(String text, Runnable action) {
            this.actionText = Objects.requireNonNull(text, "text");
            this.action = Objects.requireNonNull(action, "action");
            return this;
        }

        /**
         * Shows the toast. Safe from any thread; off the event dispatch thread
         * it appears shortly afterwards.
         *
         * @throws IllegalArgumentException if {@code owner} is not inside a Swing window
         */
        public DToast show() {
            JRootPane root = owner instanceof RootPaneContainer container
                    ? container.getRootPane()
                    : SwingUtilities.getRootPane(owner);
            if (root == null) {
                throw new IllegalArgumentException("Owner is not inside a Swing window: " + owner);
            }
            DToast toast = new DToast(this, Stack.of(root.getLayeredPane()));
            onEdt(toast::open);
            return toast;
        }
    }

    /** The toasts showing in one window, laid out per corner. */
    private static final class Stack {

        private static final String KEY = Stack.class.getName();

        private final JLayeredPane pane;
        private final List<DToast> toasts = new ArrayList<>();

        private Stack(JLayeredPane pane) {
            this.pane = pane;
            pane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    layout();
                }
            });
        }

        static Stack of(JLayeredPane pane) {
            Object existing = pane.getClientProperty(KEY);
            if (existing instanceof Stack stack) {
                return stack;
            }
            Stack stack = new Stack(pane);
            pane.putClientProperty(KEY, stack);
            return stack;
        }

        void add(DToast toast) {
            toasts.add(toast);
            pane.add(toast.panel, JLayeredPane.POPUP_LAYER);
            layout();
        }

        void remove(DToast toast) {
            toasts.remove(toast);
            pane.remove(toast.panel);
            pane.repaint(toast.panel.getBounds());
            layout();
        }

        /** Newest toast nearest the corner, older ones pushed further in; all as wide as the widest. */
        void layout() {
            for (Position position : Position.values()) {
                int width = toasts.stream().filter(t -> t.position == position)
                        .mapToInt(t -> t.panel.getPreferredSize().width).max().orElse(0);
                int offset = 0;
                for (int i = toasts.size() - 1; i >= 0; i--) {
                    DToast toast = toasts.get(i);
                    if (toast.position != position) {
                        continue;
                    }
                    Dimension size = new Dimension(width, toast.panel.getPreferredSize().height);
                    int x = switch (position) {
                        case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
                        case TOP_CENTER, BOTTOM_CENTER -> (pane.getWidth() - size.width) / 2;
                        case TOP_RIGHT, BOTTOM_RIGHT -> pane.getWidth() - size.width - MARGIN;
                    };
                    int slide = Math.round((1f - toast.opacity) * SLIDE_PX);
                    int y = position.isTop()
                            ? MARGIN + offset - slide
                            : pane.getHeight() - MARGIN - offset - size.height + slide;
                    // setBounds repaints both the old and new areas.
                    toast.panel.setBounds(x, y, size.width, size.height);
                    toast.panel.validate();
                    offset += size.height + GAP;
                }
            }
        }
    }

    /** The toast's visual: card with accent stripe, icon, text, optional action, close button. */
    private static final class ToastPanel extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final int ARC = 12;
        private static final int SHADOW = 6;

        private final transient DToast toast;
        private final Color accent;

        ToastPanel(DToast toast, Builder b) {
            super(new BorderLayout(10, 0));
            this.toast = toast;
            this.accent = b.type.accent();
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10 + SHADOW, 10 + SHADOW));

            JLabel icon = new JLabel(new TypeIcon(b.type, 20));
            icon.setVerticalAlignment(JLabel.TOP);
            add(icon, BorderLayout.WEST);

            JLabel text = new JLabel(html(b.title, b.message));
            text.setForeground(UIManager.getColor("Label.foreground"));
            add(text, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttons.setOpaque(false);
            if (b.action != null) {
                JButton action = new JButton(b.actionText);
                action.addActionListener(e -> {
                    b.action.run();
                    toast.dismiss();
                });
                buttons.add(action);
            }
            JButton close = new JButton("✕");
            close.setToolTipText("Close");
            close.getAccessibleContext().setAccessibleName("Close notification");
            close.setBorderPainted(false);
            close.setContentAreaFilled(false);
            close.setFocusPainted(false);
            close.setMargin(new Insets(0, 4, 0, 4));
            close.addActionListener(e -> toast.dismiss());
            buttons.add(close);
            add(buttons, BorderLayout.EAST);

            getAccessibleContext().setAccessibleName(b.type.name().toLowerCase(Locale.ROOT) + ": "
                    + (b.title != null ? b.title + ". " : "") + b.message);

            MouseAdapter clickToClose = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toast.dismiss();
                }
            };
            addMouseListener(clickToClose);
            text.addMouseListener(clickToClose);
        }

        private String html(String title, String message) {
            FontMetrics fm = getFontMetrics(UIManager.getFont("Label.font"));
            boolean wrap = fm.stringWidth(message) > MAX_TEXT_WIDTH
                    || title != null && fm.stringWidth(title) > MAX_TEXT_WIDTH;
            StringBuilder html = new StringBuilder("<html>");
            if (wrap) {
                html.append("<body style='width:").append(MAX_TEXT_WIDTH).append("px'>");
            }
            if (title != null) {
                html.append("<b>").append(escape(title)).append("</b><br>");
            }
            return html.append(escape(message).replace("\n", "<br>")).append("</html>").toString();
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, toast.opacity))));
                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - SHADOW;
                int h = getHeight() - SHADOW;
                for (int i = SHADOW; i > 0; i--) {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.fillRoundRect(i, i + 1, w, h, ARC + i, ARC + i);
                }
                Color panelBg = UIManager.getColor("Panel.background");
                if (panelBg == null) {
                    panelBg = Color.WHITE;
                }
                boolean dark = (panelBg.getRed() + panelBg.getGreen() + panelBg.getBlue()) / 3 < 128;
                Color card = dark ? lighten(panelBg, 0.10f) : Color.WHITE;
                g2.setColor(card);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, ARC, ARC));
                g2.setColor(accent);
                g2.fillRect(0, 0, 5, h);
                g2.setClip(null);
                Color fg = UIManager.getColor("Label.foreground");
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            } finally {
                g2.dispose();
            }
        }

        private static Color lighten(Color c, float amount) {
            return new Color(
                    Math.round(c.getRed() + (255 - c.getRed()) * amount),
                    Math.round(c.getGreen() + (255 - c.getGreen()) * amount),
                    Math.round(c.getBlue() + (255 - c.getBlue()) * amount));
        }
    }

    /** Crisp vector icons in the type's accent colour, sharp at any scale. */
    private record TypeIcon(Type type, int size) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                float s = size;
                g2.setColor(type.accent());
                if (type == Type.WARNING) {
                    Path2D triangle = new Path2D.Float();
                    triangle.moveTo(s / 2, 1);
                    triangle.lineTo(s - 1, s - 2);
                    triangle.lineTo(1, s - 2);
                    triangle.closePath();
                    g2.fill(triangle);
                } else {
                    g2.fillOval(0, 0, size, size);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(s / 9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (type) {
                    case INFO -> {
                        g2.drawLine(size / 2, Math.round(s * 0.45f), size / 2, Math.round(s * 0.72f));
                        g2.fillOval(Math.round(s / 2 - s / 14), Math.round(s * 0.22f), Math.round(s / 7), Math.round(s / 7));
                    }
                    case SUCCESS -> {
                        Path2D tick = new Path2D.Float();
                        tick.moveTo(s * 0.28, s * 0.52);
                        tick.lineTo(s * 0.44, s * 0.68);
                        tick.lineTo(s * 0.72, s * 0.36);
                        g2.draw(tick);
                    }
                    case WARNING -> {
                        g2.drawLine(size / 2, Math.round(s * 0.38f), size / 2, Math.round(s * 0.62f));
                        g2.fillOval(Math.round(s / 2 - s / 14), Math.round(s * 0.70f), Math.round(s / 7), Math.round(s / 7));
                    }
                    case ERROR -> {
                        float a = s * 0.33f;
                        float b = s * 0.67f;
                        g2.draw(new Line2D.Float(a, a, b, b));
                        g2.draw(new Line2D.Float(b, a, a, b));
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
