package com.dframework.git;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the git command lines the browser runs. Every command:
 * <ul>
 * <li>names the repository with {@code -C}, so nothing depends on the current folder
 * <li>uses {@code --no-pager}, so output is never sent to a pager that waits for keys
 * <li>runs with {@link #ENVIRONMENT}, so git never prompts and never takes optional locks
 * </ul>
 */
public final class GitCommands {

    /** The program run. Found on the PATH, as it would be in a terminal. */
    public static final String EXECUTABLE = "git";

    /**
     * Environment for every command. {@code GIT_TERMINAL_PROMPT=0}: never stop
     * to ask for a username or password. {@code GIT_OPTIONAL_LOCKS=0}: don't
     * write optional updates such as the index refresh that {@code git status}
     * would otherwise do, keeping the browser genuinely read-only.
     */
    public static final Map<String, String> ENVIRONMENT = Map.of("GIT_TERMINAL_PROMPT", "0", "GIT_OPTIONAL_LOCKS", "0");

    private GitCommands() {
    }

    /** {@code git -C <repo> --no-pager <args...>}. */
    public static List<String> command(Path repository, List<String> args) {
        List<String> command = new ArrayList<>(List.of(EXECUTABLE, "-C", repository.toString(), "--no-pager"));
        command.addAll(args);
        return command;
    }

    /** The repository's top folder: {@code rev-parse --show-toplevel}. */
    public static List<String> topLevel(Path folder) {
        return command(folder, List.of("rev-parse", "--show-toplevel"));
    }

    /** The current branch name ("HEAD" when detached): {@code rev-parse --abbrev-ref HEAD}. */
    public static List<String> currentBranch(Path repository) {
        return command(repository, List.of("rev-parse", "--abbrev-ref", "HEAD"));
    }

    /** The full object id for {@code spec}: {@code rev-parse --verify --end-of-options <spec>^{object}}. */
    public static List<String> resolve(Path repository, String spec) {
        return command(repository, List.of("rev-parse", "--verify", "--end-of-options", spec + "^{object}"));
    }

    /**
     * {@code cat-file <option> --end-of-options <spec>}, where option is
     * {@code -t} (type), {@code -s} (size) or {@code -p} (pretty-printed content).
     */
    public static List<String> catFile(Path repository, String option, String spec) {
        if (!List.of("-t", "-s", "-p").contains(option)) {
            throw new IllegalArgumentException("cat-file option must be -t, -s or -p: " + option);
        }
        return command(repository, List.of("cat-file", option, "--end-of-options", spec));
    }
}
