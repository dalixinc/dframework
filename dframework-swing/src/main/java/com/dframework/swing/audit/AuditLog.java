package com.dframework.swing.audit;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An append-only record of what an application did, for audit purposes: one
 * JSON object per line, with timestamp, user, source and details.
 *
 * <pre>{@code
 * AuditLog.getDefault().record("com.dframework.dig", "run",
 *         Map.of("command", "dig example.com MX", "exitCode", 0));
 * }</pre>
 *
 * <pre>
 * {"time":"2026-10-10T19:04:12.331+01:00","user":"dale","source":"com.dframework.dig","action":"run","command":"dig example.com MX","exitCode":"0"}
 * </pre>
 *
 * <p>The default log is {@code ~/.dframework/audit.log}; set the system
 * property {@code dframework.audit.file} or call {@link #setDefault} to move
 * it. New files are created readable by their owner only, where the file
 * system supports permissions, because commands can contain sensitive
 * details. If the file can't be written, entries are kept in memory and a
 * warning is logged; auditing never breaks the application.
 */
public final class AuditLog {

    private static final System.Logger LOG = System.getLogger(AuditLog.class.getName());
    private static final int RECENT_LIMIT = 500;

    /** One audit record. Detail values are stored as text. */
    public record Entry(Instant time, String user, String source, String action, Map<String, String> details) {

        public Entry {
            Objects.requireNonNull(time, "time");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(action, "action");
            details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
        }

        /** The entry as a single line of JSON, in the log's own format. */
        public String toJson() {
            StringBuilder json = new StringBuilder("{");
            field(json, "time", OffsetDateTime.ofInstant(time, ZoneId.systemDefault()).toString());
            field(json, "user", user);
            field(json, "source", source);
            field(json, "action", action);
            details.forEach((key, value) -> field(json, key, value));
            return json.append('}').toString();
        }

        private static void field(StringBuilder json, String key, String value) {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append('"').append(escape(key)).append("\":");
            if (value == null) {
                json.append("null");
            } else {
                json.append('"').append(escape(value)).append('"');
            }
        }
    }

    private static volatile AuditLog defaultLog;

    private final Path file;
    private final Deque<Entry> recent = new ArrayDeque<>();
    private final List<Consumer<Entry>> listeners = new CopyOnWriteArrayList<>();
    private boolean warned;

    /** A log that appends to {@code file}, creating it (and its folders) on first use. */
    public AuditLog(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    private AuditLog() {
        this.file = null;
    }

    /** A log kept only in memory, e.g. for tests or when no file is wanted. */
    public static AuditLog inMemory() {
        return new AuditLog();
    }

    /** The application-wide log. */
    public static AuditLog getDefault() {
        AuditLog log = defaultLog;
        if (log == null) {
            synchronized (AuditLog.class) {
                if (defaultLog == null) {
                    String override = System.getProperty("dframework.audit.file");
                    defaultLog = new AuditLog(override != null
                            ? Path.of(override)
                            : Path.of(System.getProperty("user.home"), ".dframework", "audit.log"));
                }
                log = defaultLog;
            }
        }
        return log;
    }

    /** Replaces the application-wide log. */
    public static void setDefault(AuditLog log) {
        defaultLog = Objects.requireNonNull(log, "log");
    }

    /** Where entries are written, or empty for an in-memory log. */
    public Optional<Path> file() {
        return Optional.ofNullable(file);
    }

    /**
     * Records an action. Safe from any thread. Detail values are converted
     * with {@code String.valueOf}; insertion order is kept.
     */
    public Entry record(String source, String action, Map<String, ?> details) {
        Map<String, String> text = new LinkedHashMap<>();
        details.forEach((key, value) -> text.put(key, value == null ? null : String.valueOf(value)));
        Entry entry = new Entry(Instant.now(), System.getProperty("user.name"), source, action, text);
        synchronized (this) {
            recent.addLast(entry);
            while (recent.size() > RECENT_LIMIT) {
                recent.removeFirst();
            }
            write(entry);
        }
        listeners.forEach(listener -> listener.accept(entry));
        return entry;
    }

    /** Entries recorded since this log was created, oldest first (at most the last 500). */
    public synchronized List<Entry> recent() {
        return List.copyOf(recent);
    }

    /** Entries from one source, oldest first. */
    public synchronized List<Entry> recent(String source) {
        List<Entry> matching = new ArrayList<>();
        for (Entry entry : recent) {
            if (entry.source().equals(source)) {
                matching.add(entry);
            }
        }
        return matching;
    }

    /** Called (on the recording thread) for every new entry. */
    public void addListener(Consumer<Entry> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(Consumer<Entry> listener) {
        listeners.remove(listener);
    }

    private void write(Entry entry) {
        if (file == null) {
            return;
        }
        try {
            if (!Files.exists(file)) {
                create(file);
            }
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
                out.write(entry.toJson());
                out.write(System.lineSeparator());
            }
        } catch (IOException | SecurityException e) {
            if (!warned) {
                warned = true;
                LOG.log(System.Logger.Level.WARNING, "Cannot write audit log " + file + "; keeping entries in memory", e);
            }
        }
    }

    private static void create(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(file);
        }
    }

    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
