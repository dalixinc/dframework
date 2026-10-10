package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dframework.swing.exec.CommandLines;

class GitPresetTest {

    private static final Path REPO = Path.of("/home/dale/repo");

    @Test
    void everyCommandNamesTheRepositoryAndDisablesThePager() {
        for (GitPreset p : GitPreset.values()) {
            List<String> command = p.command(REPO, p.input() == GitPreset.Input.PATH ? "README.md" : "", 20);
            assertThat(command.subList(0, 4)).as(p.name()).containsExactly("git", "-C", "/home/dale/repo", "--no-pager");
        }
    }

    @Test
    void onlyReadOnlyCommandsAreOffered() {
        Set<String> readOnly = Set.of("status", "log", "show", "diff", "branch", "tag", "remote", "shortlog", "reflog",
                "stash", "ls-files", "blame", "describe", "count-objects", "config");
        for (GitPreset p : GitPreset.values()) {
            List<String> command = p.command(REPO, p.input() == GitPreset.Input.PATH ? "README.md" : "", 20);
            assertThat(readOnly).as(p.name()).contains(command.get(4));
        }
        // The ones that can also write are only ever used in their listing forms.
        assertThat(GitPreset.BRANCHES.command(REPO, "", 1)).contains("--all");
        assertThat(GitPreset.TAGS.command(REPO, "", 1)).contains("--list");
        assertThat(GitPreset.STASHES.command(REPO, "", 1)).endsWith("stash", "list");
        assertThat(GitPreset.CONFIG.command(REPO, "", 1)).contains("--local", "--list");
    }

    @Test
    void limitAndDefaultRevisionAreFilledIn() {
        assertThat(CommandLines.display(GitPreset.LOG.command(REPO, "", 25)))
                .isEqualTo("git -C /home/dale/repo --no-pager log --oneline --graph --decorate -n 25");
        assertThat(GitPreset.SHOW.command(REPO, "", 5)).endsWith("--end-of-options", "HEAD");
        assertThat(GitPreset.SHOW.command(REPO, "v1.2~3", 5)).endsWith("--end-of-options", "v1.2~3");
    }

    @Test
    void pathsComeAfterTheDoubleDash() {
        assertThat(GitPreset.BLAME.command(REPO, "src/My File.java", 1)).endsWith("--", "src/My File.java");
        assertThat(GitPreset.FILE_HISTORY.command(REPO, "README.md", 7)).containsSubsequence("-n", "7", "--", "README.md");
    }

    @Test
    void valuesThatGitWouldReadAsOptionsAreRejected() {
        // --output=<file> makes some git commands write a file.
        assertThatThrownBy(() -> GitPreset.SHOW.command(REPO, "--output=/tmp/x", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GitPreset.BLAME.command(REPO, "-x", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GitPreset.SHOW.command(REPO, "HEAD; rm -rf ~", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPathIsRequiredWhereNeeded() {
        assertThatThrownBy(() -> GitPreset.BLAME.command(REPO, "  ", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theDisplayedCommandIncludesTheEnvironment() {
        assertThat(CommandLines.display(GitCommands.ENVIRONMENT, GitPreset.STATUS.command(REPO, "", 1)))
                .isEqualTo("GIT_OPTIONAL_LOCKS=0 GIT_TERMINAL_PROMPT=0 git -C /home/dale/repo --no-pager status --short --branch");
    }

    @Test
    void emptyOutputGetsAPlainEnglishNote() {
        assertThat(GitPreset.DIFF_STAGED.note(List.of())).contains("Nothing is staged.");
        assertThat(GitPreset.STASHES.note(List.of(""))).contains("No stashes.");
        assertThat(GitPreset.LOG.note(List.of("* abc123 First"))).isEmpty();
    }

    @Test
    void aCleanStatusSaysSo() {
        assertThat(GitPreset.STATUS.note(List.of("## main...origin/main")))
                .contains("Nothing to commit: the working tree is clean.");
        assertThat(GitPreset.STATUS.note(List.of("## main", " M README.md"))).isEmpty();
    }
}
