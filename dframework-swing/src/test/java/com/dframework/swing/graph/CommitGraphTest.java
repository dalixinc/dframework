package com.dframework.swing.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dframework.swing.graph.CommitGraph.Line;
import com.dframework.swing.graph.CommitGraph.Row;

class CommitGraphTest {

    private static GraphCommit commit(String id, String... parents) {
        return new GraphCommit(id, List.of(parents), List.of(), "subject " + id, "Dale", Instant.EPOCH);
    }

    @Test
    void aStraightHistoryStaysInOneLane() {
        List<Row> rows = CommitGraph.layout(List.of(commit("c", "b"), commit("b", "a"), commit("a")));

        assertThat(rows).extracting(Row::lane).containsExactly(0, 0, 0);
        assertThat(CommitGraph.maxWidth(rows)).isEqualTo(1);
        // The tip has nothing above it; the root nothing below.
        assertThat(rows.get(0).lines()).containsExactly(new Line(0, 0, 0, false));
        assertThat(rows.get(2).lines()).containsExactly(new Line(0, 0, 0, true));
    }

    @Test
    void aMergeOpensASecondLaneThatJoinsAtTheForkPoint() {
        //   m        merge of x into main
        //   |\
        //   b x
        //   |/
        //   a
        List<Row> rows = CommitGraph.layout(List.of(
                commit("m", "b", "x"), commit("b", "a"), commit("x", "a"), commit("a")));

        assertThat(rows).extracting(Row::lane).containsExactly(0, 0, 1, 0);
        assertThat(rows.get(0).lines()).contains(new Line(0, 0, 0, false), new Line(0, 1, 1, false));
        // x's line bends back into lane 0 to reach a, which b's line also heads for.
        assertThat(rows.get(2).lines()).contains(new Line(1, 0, 1, false));
        assertThat(rows.get(3).width()).isEqualTo(1);
        assertThat(CommitGraph.maxWidth(rows)).isEqualTo(2);
    }

    @Test
    void twoBranchTipsGetTheirOwnLanesAndColours() {
        List<Row> rows = CommitGraph.layout(List.of(commit("f", "a"), commit("g", "a"), commit("a")));

        assertThat(rows.get(0).lane()).isZero();
        assertThat(rows.get(1).lane()).isEqualTo(1);
        assertThat(rows.get(1).colour()).isNotEqualTo(rows.get(0).colour());
        // f's line passes g; g's line bends into it, in g's colour, so a has one line in.
        assertThat(rows.get(1).lines()).contains(new Line(0, 0, 0, true), new Line(0, 0, 0, false),
                new Line(1, 0, 1, false));
        assertThat(rows.get(2).lines()).containsExactly(new Line(0, 0, 0, true));
    }

    @Test
    void aParentBeyondTheLimitRunsOffTheBottom() {
        List<Row> rows = CommitGraph.layout(List.of(commit("b", "a")));

        assertThat(rows.get(0).lines()).containsExactly(new Line(0, 0, 0, false));
    }

    @Test
    void freedLanesAreReused() {
        // x branches off and merges back; the next branch reuses its lane.
        List<Row> rows = CommitGraph.layout(List.of(
                commit("m2", "m1", "y"), commit("y", "m1"),
                commit("m1", "a", "x"), commit("x", "a"), commit("a")));

        assertThat(rows).extracting(Row::lane).containsExactly(0, 1, 0, 1, 0);
        assertThat(CommitGraph.maxWidth(rows)).isEqualTo(2);
    }

    @Test
    void aLineOnTheRightBendsLeftSoTheMainLineKeepsItsLane() {
        // t2's line (lane 1) and a's line (lane 0) both head for p: lane 1 bends into lane 0.
        List<Row> rows = CommitGraph.layout(List.of(
                commit("t1", "a"), commit("t2", "p"), commit("a", "p"), commit("p")));

        assertThat(rows.get(2).lines()).contains(new Line(0, 0, 0, false), new Line(1, 0, 1, false))
                .doesNotContain(new Line(1, 1, 1, false));
        assertThat(rows.get(3).lane()).isZero();
        assertThat(rows.get(3).width()).isEqualTo(1);
    }
}
