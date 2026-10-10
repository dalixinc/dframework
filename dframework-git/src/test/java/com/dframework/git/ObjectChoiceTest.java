package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dframework.swing.graph.GraphCommit;

class ObjectChoiceTest {

    private static final String ID = "a1fa32567265a7240835d192f48f20c7c1c62267";

    @Test
    void offersRecentShortcutsRefsThenCommitsWithoutRepeats() {
        GraphCommit commit = new GraphCommit(ID, List.of(), List.of(), "Merge branch 'x'", "Dale", Instant.EPOCH);
        List<ObjectChoice> choices = ObjectChoice.build(List.of("HEAD:README.md", "HEAD"),
                List.of("refs/heads/main", "refs/remotes/origin/main", "refs/remotes/origin/HEAD", "refs/tags/v1.0"),
                List.of(commit));

        assertThat(choices).extracting(ObjectChoice::spec).containsExactly(
                "HEAD:README.md", "HEAD", "HEAD^{tree}", "HEAD~1", "main", "origin/main", "v1.0", ID);
        assertThat(choices.get(1).description()).isEqualTo("inspected recently");
        assertThat(choices.get(6).description()).isEqualTo("tag");
        assertThat(choices.get(7).label()).isEqualTo("a1fa325");
        assertThat(choices.get(7).description()).isEqualTo("Merge branch 'x'");
    }
}
