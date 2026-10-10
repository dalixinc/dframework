package com.dframework.swing.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs a command-line program as a separate process, <b>without a shell</b>,
 * passing each line of output on as it arrives. One runner runs one command.
 *
 * <pre>{@code
 * CommandRunner runner = new CommandRunner();
 * CommandRunner.Outcome outcome = runner.run(List.of("dig", "example.com"), Map.of(), line -> show(line));
 * }</pre>
 *
 * <p>Because no shell is involved, shell syntax in arguments ({@code ;},
 * {@code $(...)}, {@code *}) is passed on literally and never interpreted.
 * (Programs may still treat an argument starting with {@code -} as an option:
 * validate what users type.)
 */
public final class CommandRunner {

    /** By default, output beyond this many lines is not kept; the program is stopped instead. */
    public static final int DEFAULT_MAX_LINES = 20_000;

    /**
     * How a run ended.
     *
     * @param lines     everything printed (standard error included), up to the line limit
     * @param truncated whether output was cut off at the line limit (the program was then stopped)
     */
    public record Outcome(int exitCode, long durationMs, boolean cancelled, boolean truncated, List<String> lines) {
    }

    private final int maxLines;
    private volatile Process process;
    private volatile boolean cancelled;

    public CommandRunner() {
        this(DEFAULT_MAX_LINES);
    }

    /** @param maxLines output lines to keep before stopping the program */
    public CommandRunner(int maxLines) {
        if (maxLines < 1) {
            throw new IllegalArgumentException("maxLines must be positive");
        }
        this.maxLines = maxLines;
    }

    /** Finds {@code program} on the PATH, as a shell would. */
    public static Optional<Path> locate(String program) {
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
     * Runs {@code command} (program first) and waits for it to finish. Blocks,
     * so call it off the event dispatch thread.
     *
     * @param environment extra environment variables for the program, added to the current ones
     * @param onLine      called with each line of output, on this thread
     */
    public Outcome run(List<String> command, Map<String, String> environment, Consumer<String> onLine)
            throws IOException, InterruptedException {
        long start = System.nanoTime();
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().putAll(environment);
        Process p = builder.start();
        process = p;
        p.getOutputStream().close();   // nothing is ever typed into it
        List<String> lines = new ArrayList<>();
        boolean truncated = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() >= maxLines) {
                    truncated = true;
                    p.destroy();
                    break;
                }
                lines.add(line);
                onLine.accept(line);
            }
        } catch (IOException e) {
            if (!cancelled) {
                throw e;
            }
        }
        int exitCode = p.waitFor();
        return new Outcome(exitCode, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), cancelled, truncated, lines);
    }

    /** {@link #run(List, Map, Consumer)} with no extra environment. */
    public Outcome run(List<String> command, Consumer<String> onLine) throws IOException, InterruptedException {
        return run(command, Map.of(), onLine);
    }

    /** Stops the running command: politely first, then forcibly after two seconds. */
    public void cancel() {
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
