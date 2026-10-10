package com.dframework.swing.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lays out a commit history as lanes, the way {@code git log --graph} and
 * gitk do: one row per commit, with lines carrying each line of history down
 * to its parent. It only computes the layout; {@link DCommitGraph} paints it.
 *
 * <p>Commits must come children first (as {@code git log --topo-order} gives
 * them). A parent that isn't in the list, because the history was cut off at
 * a limit, gets a line that runs off the bottom.
 */
public final class CommitGraph {

    private CommitGraph() {
    }

    /**
     * One row of the layout.
     *
     * @param lane   the lane the commit's dot sits in
     * @param colour the dot's colour, as an index into the palette
     * @param lines  the lines crossing this row
     * @param width  how many lanes the row uses
     */
    public record Row(int lane, int colour, List<Line> lines, int width) {

        public Row {
            lines = List.copyOf(lines);
        }
    }

    /**
     * A line in the upper half of a row (from lane {@code from} at the top edge
     * to lane {@code to} at the middle) or the lower half (from the middle to
     * the bottom edge). A straight line through the row is an upper and a lower
     * line in the same lane.
     */
    public record Line(int from, int to, int colour, boolean upper) {
    }

    /** The layout for {@code commits}: one row each, in the same order. */
    public static List<Row> layout(List<GraphCommit> commits) {
        List<String> lanes = new ArrayList<>();      // the commit each lane is heading for; null when free
        List<Integer> colours = new ArrayList<>();
        int nextColour = 0;
        List<Row> rows = new ArrayList<>(commits.size());

        for (GraphCommit commit : commits) {
            List<Line> lines = new ArrayList<>();
            int node = lanes.indexOf(commit.id());
            boolean hasChild = node >= 0;
            if (!hasChild) {   // a branch tip: start a new line of history
                node = claim(lanes, colours, commit.id(), nextColour++);
            }
            int nodeColour = colours.get(node);

            // Upper half: lines heading for this commit join its dot; the rest pass by.
            Set<Integer> passing = new HashSet<>();
            for (int j = 0; j < lanes.size(); j++) {
                String expected = lanes.get(j);
                if (expected == null) {
                    continue;
                }
                if (expected.equals(commit.id())) {
                    if (j != node || hasChild) {
                        lines.add(new Line(j, node, colours.get(j), true));
                    }
                    if (j != node) {
                        lanes.set(j, null);
                    }
                } else {
                    lines.add(new Line(j, j, colours.get(j), true));
                    passing.add(j);
                }
            }

            // Lower half: lines from the dot to each parent.
            List<String> parents = commit.parents();
            if (parents.isEmpty()) {
                lanes.set(node, null);
            } else {
                int existing = lanes.indexOf(parents.get(0));
                if (existing >= 0 && existing < node) {
                    // A line to the left already heads for the first parent: bend into it, in this
                    // line's own colour, so the branch shows until it merges.
                    lines.add(new Line(node, existing, nodeColour, false));
                    lanes.set(node, null);
                } else if (existing > node) {
                    // A line to the right heads there too: bend that one in instead, so lines
                    // drift left and the main line stays in the leftmost lane.
                    lanes.set(node, parents.get(0));
                    lines.add(new Line(node, node, nodeColour, false));
                    lines.add(new Line(existing, node, colours.get(existing), false));
                    lanes.set(existing, null);
                    passing.remove(existing);
                } else {
                    lanes.set(node, parents.get(0));
                    lines.add(new Line(node, node, nodeColour, false));
                }
                for (String parent : parents.subList(1, parents.size())) {
                    int lane = lanes.indexOf(parent);
                    if (lane < 0) {
                        lane = claim(lanes, colours, parent, nextColour++);
                    }
                    lines.add(new Line(node, lane, colours.get(lane), false));
                }
            }
            for (int j : passing) {
                lines.add(new Line(j, j, colours.get(j), false));
            }

            int width = node + 1;
            for (Line line : lines) {
                width = Math.max(width, Math.max(line.from(), line.to()) + 1);
            }
            rows.add(new Row(node, nodeColour, lines, width));
            while (!lanes.isEmpty() && lanes.get(lanes.size() - 1) == null) {
                lanes.remove(lanes.size() - 1);
                colours.remove(colours.size() - 1);
            }
        }
        return rows;
    }

    /** The widest row, in lanes. */
    public static int maxWidth(List<Row> rows) {
        return rows.stream().mapToInt(Row::width).max().orElse(0);
    }

    /** Puts {@code id} in the first free lane (or a new one) and returns its index. */
    private static int claim(List<String> lanes, List<Integer> colours, String id, int colour) {
        int free = lanes.indexOf(null);
        if (free < 0) {
            lanes.add(id);
            colours.add(colour);
            return lanes.size() - 1;
        }
        lanes.set(free, id);
        colours.set(free, colour);
        return free;
    }
}
