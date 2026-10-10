package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dframework.swing.graph.GraphCommit;
import com.dframework.swing.graph.GraphCommit.Kind;
import com.dframework.swing.graph.GraphCommit.Ref;

class GitLogTest {

    private static final String S = GitLog.SEPARATOR;

    @Test
    void theCommandIsAReadOnlyLogInTopologicalOrder() {
        List<String> command = GitLog.command(Path.of("/repo"), true, 200);
        assertThat(command).startsWith("git", "-C", "/repo", "--no-pager", "log", "--all", "--topo-order")
                .contains("-n", "200", "--decorate=full");
        assertThat(GitLog.command(Path.of("/repo"), false, 5)).doesNotContain("--all");
        assertThatThrownBy(() -> GitLog.command(Path.of("/repo"), false, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesCommitsWithParentsRefsAndAnyCharactersInTheSubject() {
        List<GraphCommit> commits = GitLog.parse(List.of(
                "m1" + S + "b1 x1" + S + "HEAD -> refs/heads/main, refs/remotes/origin/main, refs/remotes/origin/HEAD"
                        + S + "Dale Macdonald" + S + "1791667566" + S + "Merge branch 'x': a, b | c",
                "a1" + S + "" + S + "tag: refs/tags/v1.0" + S + "Dale" + S + "1000" + S + "First",
                "not a commit line"));

        assertThat(commits).hasSize(2);
        GraphCommit merge = commits.get(0);
        assertThat(merge.parents()).containsExactly("b1", "x1");
        assertThat(merge.isMerge()).isTrue();
        assertThat(merge.refs()).containsExactly(new Ref("main", Kind.HEAD), new Ref("origin/main", Kind.REMOTE));
        assertThat(merge.subject()).isEqualTo("Merge branch 'x': a, b | c");
        assertThat(merge.time()).isEqualTo(Instant.ofEpochSecond(1791667566));
        assertThat(commits.get(1).parents()).isEmpty();
        assertThat(commits.get(1).refs()).containsExactly(new Ref("v1.0", Kind.TAG));
    }

    @Test
    void namesDetachedHeadBranchesWithSlashesAndStashes() {
        assertThat(GitLog.refs("HEAD, refs/heads/feature/login, refs/stash")).containsExactly(
                new Ref("HEAD", Kind.HEAD), new Ref("feature/login", Kind.BRANCH), new Ref("stash", Kind.OTHER));
    }
}
