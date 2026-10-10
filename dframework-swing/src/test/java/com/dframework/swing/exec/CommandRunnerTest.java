package com.dframework.swing.exec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Uses ordinary Unix programs, so it needs no network. */
class CommandRunnerTest {

    @Test
    void streamsOutputAndReportsTheExitCode() throws Exception {
        assumeTrue(CommandRunner.locate("echo").isPresent(), "needs echo");
        List<String> heard = new ArrayList<>();

        CommandRunner.Outcome outcome = new CommandRunner().run(List.of("echo", "hello", "world"), heard::add);

        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.lines()).containsExactly("hello world");
        assertThat(heard).containsExactly("hello world");
        assertThat(outcome.cancelled()).isFalse();
        assertThat(outcome.truncated()).isFalse();
    }

    @Test
    void argumentsAreNotInterpretedByAShell() throws Exception {
        assumeTrue(CommandRunner.locate("echo").isPresent(), "needs echo");

        CommandRunner.Outcome outcome = new CommandRunner().run(List.of("echo", "$(id)", ";", "*"), line -> { });

        assertThat(outcome.lines()).containsExactly("$(id) ; *");
    }

    @Test
    void passesExtraEnvironmentVariables() throws Exception {
        assumeTrue(CommandRunner.locate("env").isPresent(), "needs env");

        CommandRunner.Outcome outcome = new CommandRunner().run(List.of("env"), Map.of("DFRAMEWORK_TEST", "yes"), line -> { });

        assertThat(outcome.lines()).contains("DFRAMEWORK_TEST=yes");
    }

    @Test
    void stopsAProgramThatPrintsTooMuch() throws Exception {
        assumeTrue(CommandRunner.locate("yes").isPresent(), "needs yes");

        CommandRunner.Outcome outcome = new CommandRunner(1_000).run(List.of("yes"), line -> { });

        assertThat(outcome.truncated()).isTrue();
        assertThat(outcome.lines()).hasSize(1_000);
    }

    @Test
    void cancelStopsALongRunningCommand() throws Exception {
        assumeTrue(CommandRunner.locate("sleep").isPresent(), "needs sleep");
        CommandRunner runner = new CommandRunner();
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
        CommandRunner.Outcome outcome = runner.run(List.of("sleep", "30"), line -> { });

        assertThat(outcome.cancelled()).isTrue();
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
    }

    @Test
    void findsProgramsOnThePath() {
        assumeTrue(System.getenv("PATH") != null);
        assertThat(CommandRunner.locate("definitely-not-a-real-program-dframework")).isEmpty();
    }
}
