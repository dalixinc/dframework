package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class GitObjectsTest {

    @Test
    void parsesTreeEntriesIncludingNamesWithSpaces() {
        List<GitObjects.TreeEntry> entries = GitObjects.parseTree(List.of(
                "100644 blob 41bd0be46012bfe25ac6499ab9ffaf9cde2fe9b5\t.gitattributes",
                "040000 tree 4b825dc642cb6eb9a060e54bf8d69288fbee4904\tdocs",
                "100755 blob 1111111111111111111111111111111111111111\tMy Script.sh",
                "garbage"));

        assertThat(entries).containsExactly(
                new GitObjects.TreeEntry("100644", "blob", "41bd0be46012bfe25ac6499ab9ffaf9cde2fe9b5", ".gitattributes"),
                new GitObjects.TreeEntry("040000", "tree", "4b825dc642cb6eb9a060e54bf8d69288fbee4904", "docs"),
                new GitObjects.TreeEntry("100755", "blob", "1111111111111111111111111111111111111111", "My Script.sh"));
    }

    @Test
    void readsLinksFromTheHeaderOnly() {
        String a = "a".repeat(40);
        String b = "b".repeat(40);
        List<GitObjects.Link> links = GitObjects.parseLinks(List.of(
                "tree " + a, "parent " + b, "author Dale <d@x> 1 +0000", "", "tree " + "c".repeat(40) + " is in the message"));

        assertThat(links).containsExactly(new GitObjects.Link("tree", a), new GitObjects.Link("parent", b));
    }

    @Test
    void spotsBinaryContent() {
        assertThat(GitObjects.looksBinary(List.of("hello", "wor\u0000ld"))).isTrue();
        assertThat(GitObjects.looksBinary(List.of("café"))).isFalse();
    }
}
