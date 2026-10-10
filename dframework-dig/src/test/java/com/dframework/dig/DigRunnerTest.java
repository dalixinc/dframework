package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Uses ordinary Unix programs in place of dig, so it needs no network. */
class DigRunnerTest {

    @Test
    void streamsOutputAndReportsTheExitCode() throws Exception {
        assumeTrue(DigRunner.locate("echo").isPresent(), "needs echo");
        List<String> heard = new ArrayList<>();

        DigRunner.Outcome outcome = new DigRunner().run(List.of("echo", "hello", "world"), heard::add);

        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.lines()).containsExactly("hello world");
        assertThat(heard).containsExactly("hello world");
        assertThat(outcome.cancelled()).isFalse();
    }

    @Test
    void argumentsAreNotInterpretedByAShell() throws Exception {
        assumeTrue(DigRunner.locate("echo").isPresent(), "needs echo");

        DigRunner.Outcome outcome = new DigRunner().run(List.of("echo", "$(id)", ";", "*"), line -> { });

        assertThat(outcome.lines()).containsExactly("$(id) ; *");
    }

    @Test
    void cancelStopsALongRunningCommand() throws Exception {
        assumeTrue(DigRunner.locate("sleep").isPresent(), "needs sleep");
        DigRunner runner = new DigRunner();
        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            runner.cancel();
        });
        canceller.start();

        long start = System.nanoTime();
        DigRunner.Outcome outcome = runner.run(List.of("sleep", "30"), line -> { });

        assertThat(outcome.cancelled()).isTrue();
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
    }

    @Test
    void findsProgramsOnThePath() {
        assumeTrue(System.getenv("PATH") != null);
        assertThat(DigRunner.locate("definitely-not-a-real-program-dframework")).isEmpty();
    }
}
