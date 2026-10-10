package com.dframework.swing.theme;

import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Installs {@link DTheme}s and keeps every open window in step.
 *
 * <p>A Swing look and feel is global to the application, so this is a static
 * facade rather than something owned by a single frame. Switching theme at
 * runtime restyles every existing window and notifies registered listeners.
 *
 * <p>Successor to the 2012 {@code DLookAndFeelManager}.
 */
public final class ThemeManager {

    private static final System.Logger LOG = System.getLogger(ThemeManager.class.getName());

    private static final List<Consumer<DTheme>> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile DTheme current;

    private ThemeManager() {
    }

    /**
     * Installs {@code theme} and updates all open windows. Safe to call from
     * any thread; the work is done on the event dispatch thread.
     *
     * @param theme the theme to install
     * @throws ThemeException if the look and feel is missing or unsupported here
     */
    public static void apply(DTheme theme) {
        Objects.requireNonNull(theme, "theme");
        runOnEdt(() -> install(theme));
    }

    /**
     * Installs {@code preferred} if available, otherwise {@code fallback}.
     *
     * @return the theme actually installed
     */
    public static DTheme applyOrFallback(DTheme preferred, DTheme fallback) {
        DTheme chosen = preferred.isAvailable() ? preferred : fallback;
        if (chosen != preferred) {
            LOG.log(System.Logger.Level.INFO, "{0} is not available, using {1}", preferred, fallback);
        }
        apply(chosen);
        return chosen;
    }

    /** The theme most recently installed through this class, if any. */
    public static Optional<DTheme> current() {
        return Optional.ofNullable(current);
    }

    /** Registers a listener that is called on the EDT after each theme change. */
    public static void addThemeListener(Consumer<DTheme> listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeThemeListener(Consumer<DTheme> listener) {
        LISTENERS.remove(listener);
    }

    private static void install(DTheme theme) {
        try {
            theme.prepare();
            UIManager.setLookAndFeel(theme.className());
        } catch (ReflectiveOperationException | LinkageError | UnsupportedLookAndFeelException e) {
            throw new ThemeException("Cannot install theme " + theme + " (" + theme.className() + ")", e);
        }
        current = theme;
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        LISTENERS.forEach(listener -> listener.accept(theme));
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while switching theme", e);
        }
    }
}
