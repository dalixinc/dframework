package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitInputTest {

    @Test
    void revisions() {
        for (String ok : new String[] {"HEAD", "HEAD~2", "main^", "v1.2.0", "origin/main", "a1b2c3d", "HEAD@{1}", "HEAD^{tree}", "v1..v2"}) {
            assertThat(GitInput.revisionValidator().validate(ok)).as(ok).isEmpty();
        }
        for (String bad : new String[] {"", "--output=x", "-p", "HEAD main", "a;b", "$(id)"}) {
            assertThat(GitInput.revisionValidator().validate(bad)).as(bad).isPresent();
        }
    }

    @Test
    void objects() {
        for (String ok : new String[] {"HEAD", "HEAD:README.md", "HEAD:docs/My Notes.md", "main^{tree}", "4b825dc642cb6eb9a060e54bf8d69288fbee4904"}) {
            assertThat(GitInput.objectValidator().validate(ok)).as(ok).isEmpty();
        }
        for (String bad : new String[] {"", "--batch", "-p", "a b:c", "x\u0001"}) {
            assertThat(GitInput.objectValidator().validate(bad)).as(bad).isPresent();
        }
    }

    @Test
    void typingFilters() {
        assertThat(GitInput.revisionFilter().accept("-")).isFalse();
        assertThat(GitInput.revisionFilter().accept("HEAD~1")).isTrue();
        assertThat(GitInput.revisionFilter().accept("HEAD 1")).isFalse();
        assertThat(GitInput.pathFilter().accept("-rf")).isFalse();
        assertThat(GitInput.pathFilter().accept("My File.txt")).isTrue();
        assertThat(GitInput.objectFilter().accept("HEAD:My File.txt")).isTrue();
    }
}
