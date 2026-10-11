package com.dframework.fx;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/** Starts JavaFX once for the tests, and runs code on its thread. Skips the tests without a display. */
public final class FxToolkit {

    private static boolean started;

    private FxToolkit() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        try {
            Platform.startup(() -> { });
            Platform.setImplicitExit(false);
        } catch (IllegalStateException alreadyRunning) {
            // fine
        } catch (RuntimeException | UnsatisfiedLinkError noDisplay) {
            assumeTrue(false, "JavaFX can't start here: " + noDisplay);
        }
        started = true;
    }

    public static <T> T onFx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    public static void onFx(Runnable work) throws Exception {
        onFx(() -> {
            work.run();
            return null;
        });
    }
}
