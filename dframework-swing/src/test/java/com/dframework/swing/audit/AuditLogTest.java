package com.dframework.swing.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditLogTest {

    @Test
    void appendsOneJsonLinePerEntry(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("logs/audit.log");
        AuditLog log = new AuditLog(file);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("command", "dig example.com A");
        details.put("exitCode", 0);

        log.record("com.dframework.dig", "run", details);
        log.record("com.dframework.dig", "run", Map.of("command", "dig gmail.com MX"));

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).startsWith("{\"time\":\"").contains(
                "\"source\":\"com.dframework.dig\"", "\"action\":\"run\"",
                "\"command\":\"dig example.com A\",\"exitCode\":\"0\"}");
        assertThat(lines.get(0)).contains("\"user\":\"" + System.getProperty("user.name") + "\"");
    }

    @Test
    void escapesAwkwardText(@TempDir Path dir) throws Exception {
        AuditLog log = new AuditLog(dir.resolve("audit.log"));

        log.record("test", "run", Map.of("command", "dig 'quoted' \"double\" back\\slash\nnewline\ttab\u0001"));

        String line = Files.readString(dir.resolve("audit.log")).strip();
        assertThat(line).contains("dig 'quoted' \\\"double\\\" back\\\\slash\\nnewline\\ttab\\u0001");
        assertThat(line.lines()).hasSize(1);
    }

    @Test
    void newFilesAreReadableByTheOwnerOnly(@TempDir Path dir) throws Exception {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        Path file = dir.resolve("audit.log");
        new AuditLog(file).record("test", "run", Map.of());

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
    }

    @Test
    void keepsRecentEntriesAndNotifiesListeners() {
        AuditLog log = AuditLog.inMemory();
        List<AuditLog.Entry> heard = new ArrayList<>();
        log.addListener(heard::add);

        log.record("a", "run", Map.of());
        log.record("b", "run", Map.of());
        log.record("a", "stop", Map.of());

        assertThat(log.recent()).hasSize(3);
        assertThat(log.recent("a")).extracting(AuditLog.Entry::action).containsExactly("run", "stop");
        assertThat(heard).hasSize(3);
        assertThat(log.file()).isEmpty();
    }

    @Test
    void anUnwritableFileNeverBreaksTheApplication(@TempDir Path dir) throws Exception {
        Path blocker = dir.resolve("not-a-folder");
        Files.writeString(blocker, "x");
        AuditLog log = new AuditLog(blocker.resolve("audit.log"));   // its "folder" is a file

        AuditLog.Entry entry = log.record("test", "run", Map.of("k", "v"));

        assertThat(entry.details()).containsEntry("k", "v");
        assertThat(log.recent()).containsExactly(entry);
    }
}
