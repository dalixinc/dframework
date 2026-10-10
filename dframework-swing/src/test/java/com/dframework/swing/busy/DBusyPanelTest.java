package com.dframework.swing.busy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.junit.jupiter.api.Test;

class DBusyPanelTest {

    private final DBusyPanel busy = new DBusyPanel(new JButton("Content"));

    @Test
    void startAndStop() throws Exception {
        busy.setShowDelay(0);
        SwingUtilities.invokeAndWait(() -> busy.start("Loading"));
        assertThat(busy.isBusy()).isTrue();
        assertThat(busy.isBlocking()).isTrue();

        SwingUtilities.invokeAndWait(busy::stop);
        assertThat(busy.isBusy()).isFalse();
        assertThat(busy.isBlocking()).isFalse();
    }

    @Test
    void nestedStartsNeedMatchingStops() throws Exception {
        busy.setShowDelay(0);
        SwingUtilities.invokeAndWait(() -> {
            busy.start("One");
            busy.start("Two");
            busy.stop();
        });
        assertThat(busy.isBusy()).isTrue();

        SwingUtilities.invokeAndWait(busy::stop);
        assertThat(busy.isBusy()).isFalse();
    }

    @Test
    void quickJobsNeverShowTheOverlay() throws Exception {
        busy.setShowDelay(300);
        SwingUtilities.invokeAndWait(() -> busy.start("Quick"));
        assertThat(busy.isBusy()).isTrue();
        assertThat(busy.isBlocking()).as("still within the show delay").isFalse();

        SwingUtilities.invokeAndWait(busy::stop);
        Thread.sleep(450);
        SwingUtilities.invokeAndWait(() -> { });
        assertThat(busy.isBlocking()).isFalse();
    }

    @Test
    void runDeliversTheResultOnTheEdt() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Boolean> onEdt = new AtomicReference<>();

        busy.run("Working", progress -> {
            progress.setProgress(50);
            return "calls loaded";
        }, value -> {
            result.set(value);
            onEdt.set(SwingUtilities.isEventDispatchThread());
            done.countDown();
        });

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result).hasValue("calls loaded");
        assertThat(onEdt).hasValue(true);
        assertThat(busy.isBusy()).isFalse();
    }

    @Test
    void failuresReachTheFailureHandler() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        busy.run("Working", false, progress -> {
            throw new IllegalStateException("database offline");
        }, value -> { }, error -> {
            failure.set(error);
            done.countDown();
        });

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class).hasMessage("database offline");
        assertThat(busy.isBusy()).isFalse();
    }

    @Test
    void cancelledTasksSkipTheSuccessHandler() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Boolean> succeeded = new AtomicReference<>(false);
        AtomicReference<Boolean> sawCancel = new AtomicReference<>(false);

        SwingWorker<String, Void> worker = busy.run("Long job", true, progress -> {
            started.countDown();
            while (!progress.isCancelled()) {
                Thread.onSpinWait();
            }
            sawCancel.set(true);
            return "never used";
        }, value -> succeeded.set(true), null);

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        worker.cancel(false);   // what Esc does (with interruption)
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> { });

        assertThat(sawCancel).hasValue(true);
        assertThat(succeeded).hasValue(false);
        assertThat(busy.isBusy()).isFalse();
    }
}
