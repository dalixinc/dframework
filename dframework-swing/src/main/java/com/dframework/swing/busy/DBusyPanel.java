package com.dframework.swing.busy;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Arc2D;
import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;

/**
 * Wraps a component so it can be covered with a "busy" overlay (dimmed, with a
 * spinner or progress ring and a message) that blocks mouse and keyboard
 * input to it while work runs. The rest of the window stays usable.
 *
 * <pre>{@code
 * DBusyPanel busy = new DBusyPanel(callsScreen);   // add busy where callsScreen went
 * busy.run("Loading calls…",
 *         progress -> repository.loadCalls(progress::setProgress),   // background thread
 *         calls -> table.setModel(calls));                           // back on the EDT
 * }</pre>
 *
 * <p>Or drive it by hand with {@link #start(String)} and {@link #stop()}. Calls
 * nest: the overlay stays until every {@code start} has had its {@code stop}.
 * It appears after a short delay, so quick jobs do not flicker.
 */
public class DBusyPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final System.Logger LOG = System.getLogger(DBusyPanel.class.getName());

    /** Work to run in the background. */
    @FunctionalInterface
    public interface Task<T> {
        T call(Progress progress) throws Exception;
    }

    /** Lets a running {@link Task} report back. Safe to call from any thread. */
    public interface Progress {
        /** 0 to 100, or -1 for "unknown" (spinner). */
        void setProgress(int percent);

        void setMessage(String message);

        /** True once the user has pressed Esc on a cancellable task. Check it in loops. */
        boolean isCancelled();
    }

    // JLayer is final, so the panel holds one rather than extending it.
    private final JLayer<JComponent> layer;
    private final BusyUI ui = new BusyUI();

    public DBusyPanel(JComponent view) {
        super(new BorderLayout());
        layer = new JLayer<>(Objects.requireNonNull(view, "view"), ui);
        add(layer, BorderLayout.CENTER);
    }

    /** The wrapped component. */
    public JComponent getView() {
        return layer.getView();
    }

    private BusyUI busyUI() {
        return ui;
    }

    /** Shows the overlay (after the show delay) with {@code message}. */
    public void start(String message) {
        onEdt(() -> busyUI().start(layer, message));
    }

    /** Ends one {@link #start}; the overlay goes when every start has ended. */
    public void stop() {
        onEdt(() -> busyUI().stop(layer));
    }

    /** Whether the overlay is active (including during the show delay). */
    public boolean isBusy() {
        return busyUI().depth > 0;
    }

    /** Whether input is currently blocked, i.e. the overlay is actually showing. */
    public boolean isBlocking() {
        return busyUI().visible;
    }

    public void setMessage(String message) {
        onEdt(() -> {
            busyUI().message = message;
            repaint();
        });
    }

    /** 0 to 100 shows a progress ring; -1 (the default) shows a spinner. */
    public void setProgress(int percent) {
        onEdt(() -> {
            busyUI().progress = percent < 0 ? -1 : Math.min(100, percent);
            repaint();
        });
    }

    /** Milliseconds to wait before showing the overlay, so quick jobs don't flash it. Default 200. */
    public void setShowDelay(int millis) {
        busyUI().showDelay.setInitialDelay(Math.max(0, millis));
    }

    /**
     * Runs {@code task} on a background thread with the overlay up.
     *
     * @param onSuccess called on the EDT with the result
     * @return the worker, e.g. to cancel it
     */
    public <T> SwingWorker<T, Void> run(String message, Task<T> task, Consumer<? super T> onSuccess) {
        return run(message, false, task, onSuccess, null);
    }

    /**
     * Runs {@code task} on a background thread with the overlay up.
     *
     * @param cancellable whether <kbd>Esc</kbd> cancels it (the task should check
     *                    {@link Progress#isCancelled()})
     * @param onSuccess   called on the EDT with the result (not called if cancelled)
     * @param onFailure   called on the EDT with the exception the task threw;
     *                    {@code null} logs it instead
     */
    public <T> SwingWorker<T, Void> run(String message, boolean cancellable, Task<T> task,
            Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure) {
        Objects.requireNonNull(task, "task");
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            private final Progress progress = new Progress() {
                @Override
                public void setProgress(int percent) {
                    DBusyPanel.this.setProgress(percent);
                }

                @Override
                public void setMessage(String text) {
                    DBusyPanel.this.setMessage(text);
                }

                @Override
                public boolean isCancelled() {
                    // A bare isCancelled() here would call this method itself, not the worker's.
                    return workerCancelled();
                }
            };

            private boolean workerCancelled() {
                return isCancelled();
            }

            @Override
            protected T doInBackground() throws Exception {
                return task.call(progress);
            }

            @Override
            protected void done() {
                busyUI().cancel = null;
                stop();
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (CancellationException e) {
                    // The user cancelled; nothing to report.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    if (onFailure != null) {
                        onFailure.accept(e.getCause());
                    } else {
                        LOG.log(System.Logger.Level.ERROR, "Background task failed: " + message, e.getCause());
                    }
                }
            }
        };
        onEdt(() -> {
            busyUI().progress = -1;
            busyUI().cancel = cancellable ? () -> worker.cancel(true) : null;
            busyUI().start(layer, message);
            worker.execute();
        });
        return worker;
    }

    private static void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /** Paints the overlay and swallows input while it shows. */
    private static final class BusyUI extends LayerUI<JComponent> {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final int SPOKES = 12;
        private static final int FADE_MS = 160;

        private final Timer showDelay;
        private final Timer animation;
        private JLayer<?> layer;
        private int depth;
        private boolean visible;
        private float fade;
        private int frame;
        private String message;
        private int progress = -1;
        private transient Runnable cancel;

        BusyUI() {
            showDelay = new Timer(200, e -> reveal());
            showDelay.setRepeats(false);
            animation = new Timer(50, e -> tick());
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            ((JLayer<?>) c).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        }

        @Override
        public void uninstallUI(JComponent c) {
            ((JLayer<?>) c).setLayerEventMask(0);
            showDelay.stop();
            animation.stop();
            super.uninstallUI(c);
        }

        void start(JLayer<?> l, String text) {
            layer = l;
            message = text;
            if (depth++ == 0) {
                if (showDelay.getInitialDelay() == 0) {
                    reveal();
                } else {
                    showDelay.restart();
                }
            }
            l.repaint();
        }

        void stop(JLayer<?> l) {
            if (depth == 0) {
                return;
            }
            if (--depth == 0) {
                showDelay.stop();
                visible = false;
                fade = 0;
                animation.stop();
                cancel = null;
                l.repaint();
            }
        }

        private void reveal() {
            if (depth == 0) {
                return;
            }
            visible = true;
            fade = 0;
            // Take focus off the blocked content so typing can't reach it.
            if (layer != null && layer.getView() != null) {
                var owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner != null && SwingUtilities.isDescendingFrom(owner, layer.getView())) {
                    layer.requestFocusInWindow();
                }
            }
            animation.start();
            layer.repaint();
        }

        private void tick() {
            frame = (frame + 1) % SPOKES;
            fade = Math.min(1f, fade + animation.getDelay() / (float) FADE_MS);
            if (layer != null) {
                layer.repaint();
            }
        }

        @Override
        protected void processKeyEvent(KeyEvent e, JLayer<? extends JComponent> l) {
            if (visible && cancel != null && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                Runnable c = cancel;
                cancel = null;
                c.run();
            }
        }

        @Override
        public void eventDispatched(AWTEvent e, JLayer<? extends JComponent> l) {
            super.eventDispatched(e, l);
            if (visible && e instanceof InputEvent input) {
                input.consume();
            }
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (!visible) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.SrcOver.derive(fade));
                int w = c.getWidth();
                int h = c.getHeight();
                Color bg = UIManager.getColor("Panel.background");
                Color fg = UIManager.getColor("Label.foreground");
                if (bg == null) {
                    bg = Color.WHITE;
                }
                if (fg == null) {
                    fg = Color.BLACK;
                }
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 170));
                g2.fillRect(0, 0, w, h);

                Font font = UIManager.getFont("Label.font");
                if (font != null) {
                    g2.setFont(font);
                }
                FontMetrics fm = g2.getFontMetrics();
                String hint = cancel != null ? "Press Esc to cancel" : null;
                int size = 40;
                int textWidth = Math.max(message == null ? 0 : fm.stringWidth(message), hint == null ? 0 : fm.stringWidth(hint));
                int cardW = Math.max(size, textWidth) + 48;
                int cardH = size + 32 + (message == null ? 0 : fm.getHeight() + 8) + (hint == null ? 0 : fm.getHeight());
                int cx = (w - cardW) / 2;
                int cy = (h - cardH) / 2;
                boolean dark = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 < 128;
                g2.setColor(dark ? blend(bg, Color.WHITE, 0.08f) : Color.WHITE);
                g2.fillRoundRect(cx, cy, cardW, cardH, 16, 16);
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
                g2.drawRoundRect(cx, cy, cardW - 1, cardH - 1, 16, 16);

                int sx = (w - size) / 2;
                int sy = cy + 16;
                Color accent = accent();
                if (progress < 0) {
                    paintSpinner(g2, sx, sy, size, fg);
                } else {
                    paintRing(g2, sx, sy, size, accent, fg, fm);
                }
                int y = sy + size + 8 + fm.getAscent();
                if (message != null) {
                    g2.setColor(fg);
                    g2.drawString(message, (w - fm.stringWidth(message)) / 2, y);
                    y += fm.getHeight();
                }
                if (hint != null) {
                    g2.setColor(blend(fg, bg, 0.45f));
                    g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, y);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintSpinner(Graphics2D g2, int x, int y, int size, Color fg) {
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            g2.setStroke(new BasicStroke(size / 11f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            for (int i = 0; i < SPOKES; i++) {
                // The spoke at the current frame is solid; the ones behind it fade out.
                int age = Math.floorMod(frame - i, SPOKES);
                int alpha = Math.max(30, 255 - age * 20);
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), alpha));
                double angle = 2 * Math.PI * i / SPOKES;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                g2.drawLine(Math.round((float) (cx + cos * size * 0.25)), Math.round((float) (cy + sin * size * 0.25)),
                        Math.round((float) (cx + cos * size * 0.45)), Math.round((float) (cy + sin * size * 0.45)));
            }
        }

        private void paintRing(Graphics2D g2, int x, int y, int size, Color accent, Color fg, FontMetrics fm) {
            float stroke = size / 9f;
            float inset = stroke / 2;
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
            g2.draw(new Arc2D.Float(x + inset, y + inset, size - stroke, size - stroke, 0, 360, Arc2D.OPEN));
            g2.setColor(accent);
            g2.draw(new Arc2D.Float(x + inset, y + inset, size - stroke, size - stroke, 90, -3.6f * progress, Arc2D.OPEN));
            String pct = progress + "%";
            Font small = g2.getFont().deriveFont(g2.getFont().getSize2D() * 0.8f);
            FontMetrics sm = g2.getFontMetrics(small);
            g2.setFont(small);
            g2.setColor(fg);
            g2.drawString(pct, x + (size - sm.stringWidth(pct)) / 2f, y + (size - sm.getHeight()) / 2f + sm.getAscent());
            g2.setFont(fm.getFont());
        }

        private static Color accent() {
            for (String key : new String[] {"Component.accentColor", "ProgressBar.foreground", "textHighlight"}) {
                Color c = UIManager.getColor(key);
                if (c != null) {
                    return c;
                }
            }
            return new Color(0x2F80ED);
        }

        private static Color blend(Color a, Color b, float amount) {
            return new Color(
                    Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
        }
    }
}
