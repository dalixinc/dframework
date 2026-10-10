package com.dframework.dig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs a command (normally dig) as a separate process, without a shell,
 * passing each line of output on as it arrives. One runner runs one command.
 */
final class DigRunner {

    /** How a run ended. {@code lines} is everything printed, standard error included. */
    record Outcome(int exitCode, long durationMs, boolean cancelled, List<String> lines) {
    }

    private volatile Process process;
    private volatile boolean cancelled;

    /** Finds {@code program} on the PATH, as a shell would. */
    static Optional<Path> locate(String program) {
        String path = System.getenv("PATH");
        if (path == null) {
            return Optional.empty();
        }
        boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            for (String name : windows ? List.of(program + ".exe", program) : List.of(program)) {
                Path candidate = Path.of(dir, name);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Runs {@code command} and waits for it to finish. Blocks, so call it off
     * the event dispatch thread.
     */
    Outcome run(List<String> command, Consumer<String> onLine) throws IOException, InterruptedException {
        long start = System.nanoTime();
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        process = p;
        p.getOutputStream().close();   // dig reads nothing from us
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                onLine.accept(line);
            }
        } catch (IOException e) {
            if (!cancelled) {
                throw e;
            }
        }
        int exitCode = p.waitFor();
        return new Outcome(exitCode, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), cancelled, lines);
    }

    /** Stops the running command: politely first, then forcibly after two seconds. */
    void cancel() {
        cancelled = true;
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroy();
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                if (p.isAlive()) {
                    p.destroyForcibly();
                }
            });
        }
    }
}
