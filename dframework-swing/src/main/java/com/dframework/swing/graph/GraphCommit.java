package com.dframework.swing.graph;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One commit in a {@link DCommitGraph}: its id, its parents' ids, and what to
 * show about it. Nothing here is specific to git; any history made of commits
 * with parents (Mercurial, a document's revisions) fits.
 *
 * @param id      unique id
 * @param parents parent ids, first parent first; empty for a root commit
 * @param refs    branch, tag and HEAD labels pointing at this commit
 * @param subject one-line description
 * @param author  who made it
 * @param time    when it was made
 */
public record GraphCommit(String id, List<String> parents, List<Ref> refs, String subject, String author, Instant time) {

    public GraphCommit {
        Objects.requireNonNull(id, "id");
        parents = List.copyOf(parents);
        refs = List.copyOf(refs);
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(time, "time");
    }

    /** Whether this commit joins two or more lines of history. */
    public boolean isMerge() {
        return parents.size() > 1;
    }

    /** A label pointing at a commit, drawn as a badge. */
    public record Ref(String name, Kind kind) {

        public Ref {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** What a ref is, which decides its badge colour. */
    public enum Kind {
        /** The checked-out branch (or "HEAD" when detached). */
        HEAD,
        /** A local branch. */
        BRANCH,
        /** A remote-tracking branch, such as origin/main. */
        REMOTE,
        /** A tag. */
        TAG,
        /** Anything else, such as a stash. */
        OTHER
    }
}
