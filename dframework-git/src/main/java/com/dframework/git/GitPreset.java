package com.dframework.git;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The common, read-only git commands offered in the browser's menu. There is
 * deliberately no way to type an arbitrary git command: none of these can
 * change the repository.
 *
 * <p>A preset may take one value from the user: a revision, such as
 * {@code HEAD~2} or a branch, or a file path. Revisions follow
 * {@code --end-of-options} and paths follow {@code --}, so git always reads
 * them as a revision or path and never as an option, even before validation.
 */
public enum GitPreset {

    STATUS("Status", "Changed files and the current branch", Input.NONE, "status", "--short", "--branch"),
    LOG("Recent commits", "The latest commits on this branch, as a graph", Input.NONE,
            "log", "--oneline", "--graph", "--decorate", "-n", "{limit}"),
    LOG_ALL("All branches", "Recent commits on every branch, as a graph", Input.NONE,
            "log", "--oneline", "--graph", "--decorate", "--all", "-n", "{limit}"),
    SHOW("Show a commit", "One commit in full: author, message and changed files", Input.REVISION,
            "show", "--stat", "--format=fuller", "--end-of-options", "{revision}"),
    DIFF("Unstaged changes", "Files changed since they were last staged", Input.NONE, "diff", "--stat"),
    DIFF_STAGED("Staged changes", "What the next commit would contain", Input.NONE, "diff", "--cached", "--stat"),
    BRANCHES("Branches", "Local and remote branches, with what they track", Input.NONE, "branch", "--all", "-vv"),
    TAGS("Tags", "Tags, with the first line of each tag's message", Input.NONE, "tag", "--list", "-n1"),
    REMOTES("Remotes", "Where this repository fetches from and pushes to", Input.NONE, "remote", "-v"),
    CONTRIBUTORS("Contributors", "Authors by number of commits", Input.NONE, "shortlog", "-sne", "HEAD"),
    REFLOG("Reflog", "Where HEAD has pointed recently, including undone work", Input.NONE,
            "reflog", "-n", "{limit}"),
    STASHES("Stashes", "Work put aside with git stash", Input.NONE, "stash", "list"),
    FILES("Tracked files", "Every file git is tracking", Input.NONE, "ls-files"),
    FILE_HISTORY("History of a file", "Commits that changed a file, following renames", Input.PATH,
            "log", "--oneline", "--follow", "-n", "{limit}", "--", "{path}"),
    BLAME("Blame a file", "Who last changed each line of a file", Input.PATH, "blame", "--date=short", "--", "{path}"),
    DESCRIBE("Describe", "The nearest tag, and how far HEAD is from it", Input.NONE, "describe", "--tags", "--always", "--dirty"),
    OBJECT_COUNT("Object counts", "How many objects the repository holds, and their size", Input.NONE,
            "count-objects", "-v", "-H"),
    CONFIG("Repository settings", "Settings stored in this repository's own config", Input.NONE, "config", "--local", "--list");

    /** What, if anything, the user supplies. */
    public enum Input {
        NONE, REVISION, PATH
    }

    private final String label;
    private final String description;
    private final Input input;
    private final List<String> template;

    GitPreset(String label, String description, Input input, String... template) {
        this.label = label;
        this.description = description;
        this.input = input;
        this.template = List.of(template);
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public Input input() {
        return input;
    }

    /** Whether the command uses the "limit" setting (how many entries to show). */
    public boolean usesLimit() {
        return template.contains("{limit}");
    }

    /**
     * A sentence saying what the output means when it needs saying: what an
     * empty result means for this command, or that a status shows a clean
     * working tree. Empty when the output speaks for itself.
     */
    public Optional<String> note(List<String> output) {
        boolean empty = output.stream().allMatch(String::isBlank);
        if (this == STATUS && output.size() == 1 && output.get(0).startsWith("##")) {
            return Optional.of("Nothing to commit: the working tree is clean.");
        }
        if (!empty) {
            return Optional.empty();
        }
        return Optional.of(switch (this) {
            case LOG, LOG_ALL -> "No commits yet.";
            case DIFF -> "No unstaged changes: every modified file is staged, or nothing has changed.";
            case DIFF_STAGED -> "Nothing is staged.";
            case BRANCHES -> "No branches yet.";
            case TAGS -> "No tags.";
            case REMOTES -> "No remotes are set up.";
            case CONTRIBUTORS -> "No commits yet, so no contributors.";
            case REFLOG -> "The reflog is empty.";
            case STASHES -> "No stashes.";
            case FILES -> "No tracked files.";
            case FILE_HISTORY -> "No commits changed this file.";
            case CONFIG -> "This repository has no settings of its own.";
            default -> "The command printed nothing.";
        });
    }

    /**
     * The full command line.
     *
     * @param value revision or path, as the preset needs; a blank revision means HEAD
     * @param limit how many entries, for presets that list commits
     * @throws IllegalArgumentException if the value is missing or not allowed
     */
    public List<String> command(Path repository, String value, int limit) {
        String v = value == null ? "" : value.strip();
        if (input == Input.REVISION && v.isEmpty()) {
            v = "HEAD";
        }
        if (input != Input.NONE) {
            GitInput.check(input == Input.PATH ? GitInput.pathValidator() : GitInput.revisionValidator(), v);
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        List<String> args = new ArrayList<>();
        for (String part : template) {
            args.add(switch (part) {
                case "{limit}" -> Integer.toString(limit);
                case "{revision}", "{path}" -> v;
                default -> part;
            });
        }
        return GitCommands.command(repository, args);
    }

    @Override
    public String toString() {
        return label;
    }
}
