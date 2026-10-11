package com.dframework.fx.theme;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;

/**
 * Switches the application's theme, live. JavaFX restyles every open window
 * when the user-agent stylesheet changes, so unlike Swing there's no tree of
 * components to walk.
 */
public final class FxThemeManager {

    private static final List<Consumer<FxTheme>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile FxTheme current;

    private FxThemeManager() {
    }

    /** Applies {@code theme} to the whole application. Call on the JavaFX thread. */
    public static void apply(FxTheme theme) {
        Objects.requireNonNull(theme, "theme");
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Apply themes on the JavaFX application thread");
        }
        Application.setUserAgentStylesheet(theme.stylesheet());
        current = theme;
        LISTENERS.forEach(listener -> listener.accept(theme));
    }

    /** The theme applied last, if any. */
    public static Optional<FxTheme> current() {
        return Optional.ofNullable(current);
    }

    /** Called with each newly applied theme. */
    public static void addThemeListener(Consumer<FxTheme> listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeThemeListener(Consumer<FxTheme> listener) {
        LISTENERS.remove(listener);
    }
}
