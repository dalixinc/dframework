package com.dframework.git;

import java.util.ArrayList;
import java.util.List;

/** Reads the output of {@code git cat-file -p}. */
public final class GitObjects {

    /** One line of a tree: {@code <mode> <type> <id>\t<name>}. */
    public record TreeEntry(String mode, String type, String id, String name) {
    }

    /** A link from a commit or tag to another object: e.g. ("tree", id) or ("parent", id). */
    public record Link(String kind, String id) {
    }

    private GitObjects() {
    }

    /** Entries of a tree, in git's order. Lines that don't fit the format are skipped. */
    public static List<TreeEntry> parseTree(List<String> lines) {
        List<TreeEntry> entries = new ArrayList<>();
        for (String line : lines) {
            int tab = line.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String[] head = line.substring(0, tab).split(" ");
            if (head.length == 3) {
                entries.add(new TreeEntry(head[0], head[1], head[2], line.substring(tab + 1)));
            }
        }
        return entries;
    }

    /**
     * Links in a commit ({@code tree}, {@code parent}) or tag ({@code object})
     * header, which ends at the first blank line.
     */
    public static List<Link> parseLinks(List<String> lines) {
        List<Link> links = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty()) {
                break;
            }
            String[] parts = line.split(" ", 2);
            if (parts.length == 2 && List.of("tree", "parent", "object").contains(parts[0])
                    && parts[1].matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
                links.add(new Link(parts[0], parts[1]));
            }
        }
        return links;
    }

    /** Whether text looks like binary data, so isn't worth showing as text. */
    public static boolean looksBinary(List<String> lines) {
        for (String line : lines) {
            if (line.indexOf('\u0000') >= 0 || line.indexOf('�') >= 0) {
                return true;
            }
        }
        return false;
    }
}
