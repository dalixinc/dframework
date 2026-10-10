package com.dframework.git;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dframework.swing.graph.GraphCommit;
import com.dframework.swing.graph.GraphCommit.Kind;
import com.dframework.swing.graph.GraphCommit.Ref;

/**
 * The commit graph's command and the parsing of its output. One read-only
 * {@code git log} gives every commit's id, parents, refs, author, time and
 * subject, separated by the ASCII unit separator so subjects can contain
 * anything.
 */
public final class GitLog {

    /** Field separator: the ASCII unit separator, which never appears in subjects or names. */
    static final String SEPARATOR = "\u001f";

    private GitLog() {
    }

    /**
     * {@code git log [--all] --topo-order --decorate=full -n <limit> --format=...}.
     * Topological order lists children before parents, which the graph needs;
     * full decoration names refs unambiguously ({@code refs/heads/},
     * {@code refs/remotes/}, {@code refs/tags/}).
     */
    public static List<String> command(Path repository, boolean allBranches, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("The limit must be at least 1");
        }
        List<String> args = new ArrayList<>(List.of("log"));
        if (allBranches) {
            args.add("--all");
        }
        args.addAll(List.of("--topo-order", "--decorate=full", "-n", Integer.toString(limit),
                "--format=%H%x1f%P%x1f%D%x1f%an%x1f%at%x1f%s"));
        return GitCommands.command(repository, args);
    }

    /** The commits in {@code lines}, skipping any line that isn't in the expected format. */
    public static List<GraphCommit> parse(List<String> lines) {
        List<GraphCommit> commits = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.split(SEPARATOR, 6);
            if (f.length < 6 || f[0].isBlank()) {
                continue;
            }
            long seconds;
            try {
                seconds = Long.parseLong(f[4].strip());
            } catch (NumberFormatException notATime) {
                continue;
            }
            List<String> parents = f[1].isBlank() ? List.of() : Arrays.asList(f[1].strip().split(" "));
            commits.add(new GraphCommit(f[0].strip(), parents, refs(f[2]), f[5], f[3], Instant.ofEpochSecond(seconds)));
        }
        return commits;
    }

    /**
     * Parses {@code %D} with full decoration, e.g. {@code HEAD -> refs/heads/main, tag: refs/tags/v1},
     * or full ref names as {@code for-each-ref} lists them.
     */
    static List<Ref> refs(String decoration) {
        List<Ref> refs = new ArrayList<>();
        if (decoration.isBlank()) {
            return refs;
        }
        for (String part : decoration.split(", ")) {
            String p = part.strip();
            if (p.startsWith("HEAD -> ")) {
                refs.add(new Ref(shortName(p.substring("HEAD -> ".length())), Kind.HEAD));
            } else if (p.equals("HEAD")) {
                refs.add(new Ref("HEAD", Kind.HEAD));   // detached
            } else if (p.startsWith("tag: ")) {
                refs.add(new Ref(shortName(p.substring("tag: ".length())), Kind.TAG));
            } else if (p.startsWith("refs/tags/")) {   // as for-each-ref writes it
                refs.add(new Ref(shortName(p), Kind.TAG));
            } else if (p.startsWith("refs/heads/")) {
                refs.add(new Ref(shortName(p), Kind.BRANCH));
            } else if (p.startsWith("refs/remotes/")) {
                if (!p.endsWith("/HEAD")) {   // origin/HEAD only repeats the remote's default branch
                    refs.add(new Ref(shortName(p), Kind.REMOTE));
                }
            } else if (!p.isEmpty()) {
                refs.add(new Ref(shortName(p), Kind.OTHER));
            }
        }
        return refs;
    }

    private static String shortName(String ref) {
        for (String prefix : List.of("refs/heads/", "refs/remotes/", "refs/tags/", "refs/")) {
            if (ref.startsWith(prefix)) {
                return ref.substring(prefix.length());
            }
        }
        return ref;
    }
}
