package com.dframework.git;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dframework.swing.graph.GraphCommit;

/**
 * An entry in the Objects tab's dropdown: what to inspect, and a few words
 * saying what it is.
 *
 * @param spec        what is inspected, such as {@code HEAD}, {@code main} or a commit id
 * @param description shown dimmed after it, such as "branch" or a commit's subject
 */
record ObjectChoice(String spec, String description) {

    /** The most recently inspected objects offered. */
    static final int RECENT = 8;
    /** The most commits from the graph offered. */
    static final int COMMITS = 50;

    /** What the editor shows when this is chosen. */
    @Override
    public String toString() {
        return spec;
    }

    /** The text in the list: a commit's short id, anything else in full. */
    String label() {
        return spec.matches("[0-9a-f]{40,64}") ? spec.substring(0, 7) : spec;
    }

    /**
     * The dropdown's entries, in this order, without repeats:
     * recently inspected, the HEAD shortcuts, refs, then commits from the graph.
     *
     * @param recent  specs inspected this session, newest first
     * @param refs    full ref names from {@link GitCommands#refs}
     * @param commits the graph's commits, newest first
     */
    static List<ObjectChoice> build(List<String> recent, List<String> refs, List<GraphCommit> commits) {
        Map<String, ObjectChoice> choices = new LinkedHashMap<>();
        recent.stream().limit(RECENT).forEach(spec -> choices.putIfAbsent(spec, new ObjectChoice(spec, "inspected recently")));
        for (ObjectChoice shortcut : List.of(
                new ObjectChoice("HEAD", "the commit checked out"),
                new ObjectChoice("HEAD^{tree}", "the top folder at HEAD"),
                new ObjectChoice("HEAD~1", "the commit before HEAD"))) {
            choices.putIfAbsent(shortcut.spec(), shortcut);
        }
        for (GraphCommit.Ref ref : GitLog.refs(String.join(", ", refs))) {
            if (ref.kind() == GraphCommit.Kind.OTHER) {
                continue;
            }
            String kind = switch (ref.kind()) {
                case TAG -> "tag";
                case REMOTE -> "remote branch";
                default -> "branch";
            };
            choices.putIfAbsent(ref.name(), new ObjectChoice(ref.name(), kind));
        }
        commits.stream().limit(COMMITS).forEach(c -> choices.putIfAbsent(c.id(), new ObjectChoice(c.id(), c.subject())));
        return new ArrayList<>(choices.values());
    }
}
