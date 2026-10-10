package com.dframework.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dframework.swing.exec.CommandRunner;
import com.dframework.swing.graph.CommitGraph;
import com.dframework.swing.graph.GraphCommit;

/** Runs the browser's commands with real git against a throwaway repository. */
class GitIntegrationTest {

    @TempDir
    Path repo;

    @BeforeEach
    void createRepository() throws Exception {
        assumeTrue(CommandRunner.locate("git").isPresent(), "needs git");
        Files.writeString(repo.resolve("README.md"), "# Hello\nDFramework test repository\n");
        Files.createDirectories(repo.resolve("docs"));
        Files.writeString(repo.resolve("docs/notes.txt"), "notes\n");
        git("init", "-q", "-b", "main");
        git("add", ".");
        git("-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-q", "-m", "First commit");
        git("tag", "v1.0");
    }

    private void git(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        CommandRunner.Outcome outcome = new CommandRunner().run(command, line -> { });
        assertThat(outcome.exitCode()).as(String.join(" ", args) + ": " + outcome.lines()).isZero();
    }

    private CommandRunner.Outcome run(List<String> command) throws Exception {
        return new CommandRunner().run(command, GitCommands.ENVIRONMENT, line -> { });
    }

    @Test
    void everyPresetRunsCleanly() throws Exception {
        for (GitPreset p : GitPreset.values()) {
            CommandRunner.Outcome outcome = run(p.command(repo, p.input() == GitPreset.Input.PATH ? "README.md" : "", 10));
            assertThat(outcome.exitCode()).as(p.name() + " " + outcome.lines()).isZero();
        }
        assertThat(run(GitPreset.LOG.command(repo, "", 10)).lines()).anyMatch(l -> l.contains("First commit"));
        assertThat(run(GitPreset.DESCRIBE.command(repo, "", 10)).lines()).containsExactly("v1.0");
    }

    @Test
    void walksFromTheCommitToATreeToABlob() throws Exception {
        String commit = run(GitCommands.resolve(repo, "HEAD")).lines().get(0);
        assertThat(run(GitCommands.catFile(repo, "-t", commit)).lines()).containsExactly("commit");

        List<GitObjects.Link> links = GitObjects.parseLinks(run(GitCommands.catFile(repo, "-p", commit)).lines());
        String tree = links.stream().filter(l -> l.kind().equals("tree")).findFirst().orElseThrow().id();

        List<GitObjects.TreeEntry> entries = GitObjects.parseTree(run(GitCommands.catFile(repo, "-p", tree)).lines());
        assertThat(entries).extracting(GitObjects.TreeEntry::name).containsExactly("README.md", "docs");
        String readme = entries.get(0).id();

        assertThat(run(GitCommands.catFile(repo, "-p", readme)).lines()).containsExactly("# Hello", "DFramework test repository");
        assertThat(run(GitCommands.catFile(repo, "-s", readme)).lines()).containsExactly("35");
        assertThat(run(GitCommands.catFile(repo, "-p", "HEAD:docs/notes.txt")).lines()).containsExactly("notes");
    }

    @Test
    void statusDoesNotRewriteTheIndex() throws Exception {
        // A changed timestamp makes a plain "git status" refresh and rewrite the index.
        Files.setLastModifiedTime(repo.resolve("README.md"), FileTime.fromMillis(System.currentTimeMillis() + 60_000));
        Path index = repo.resolve(".git/index");
        byte[] before = Files.readAllBytes(index);

        run(GitPreset.STATUS.command(repo, "", 1));

        assertThat(Files.readAllBytes(index)).as("index untouched").isEqualTo(before);
    }

    @Test
    void anOptionSmuggledInAsARevisionIsRefusedByGitToo() throws Exception {
        Path target = repo.resolve("written-by-git.txt");
        // Validation rejects this, but even if it got through, --end-of-options stops git treating it as an option.
        List<String> command = GitCommands.command(repo, List.of("show", "--end-of-options", "--output=" + target));

        CommandRunner.Outcome outcome = run(command);

        assertThat(outcome.exitCode()).isNotZero();
        assertThat(target).doesNotExist();
    }

    @Test
    void theGraphLogParsesABranchAndAMerge() throws Exception {
        String[] who = {"-c", "user.name=Test", "-c", "user.email=test@example.com"};
        git("checkout", "-q", "-b", "feature");
        Files.writeString(repo.resolve("feature.txt"), "new\n");
        git("add", ".");
        git(concat(who, "commit", "-q", "-m", "Add feature"));
        git("checkout", "-q", "main");
        Files.writeString(repo.resolve("main.txt"), "main\n");
        git("add", ".");
        git(concat(who, "commit", "-q", "-m", "Work on main"));
        git(concat(who, "merge", "-q", "--no-ff", "-m", "Merge feature", "feature"));

        List<GraphCommit> commits = GitLog.parse(run(GitLog.command(repo, true, 50)).lines());

        // Children before parents; git chooses which side of the merge comes first.
        assertThat(commits).extracting(GraphCommit::subject)
                .containsExactlyInAnyOrder("Merge feature", "Work on main", "Add feature", "First commit")
                .startsWith("Merge feature").endsWith("First commit");
        assertThat(commits.get(0).isMerge()).isTrue();
        assertThat(commits.get(0).refs()).extracting(GraphCommit.Ref::name).containsExactly("main");
        assertThat(commits.get(3).refs()).extracting(GraphCommit.Ref::name).containsExactly("v1.0");
        assertThat(CommitGraph.maxWidth(CommitGraph.layout(commits)))
                .isEqualTo(2);
    }

    private static String[] concat(String[] first, String... rest) {
        String[] all = Arrays.copyOf(first, first.length + rest.length);
        System.arraycopy(rest, 0, all, first.length, rest.length);
        return all;
    }
}
