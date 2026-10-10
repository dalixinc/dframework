package com.dframework.time;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Compiles a snippet of Java with the JDK's own compiler, in memory, and runs
 * it, capturing what it prints. What is shown on screen is exactly what runs,
 * and the presenter can edit it live.
 *
 * <p>A snippet is the body of a method {@code run(PrintStream out)}: it prints
 * with {@code out.println}. Lines at its start beginning {@code import} become
 * imports. The common date and time packages are imported already (see
 * {@link #DEFAULT_IMPORTS}); a single-type import such as
 * {@code import org.joda.time.DateTime;} takes precedence over them.
 */
public final class SnippetRunner {

    /** Imported into every snippet. */
    public static final List<String> DEFAULT_IMPORTS = List.of(
            "java.io.*", "java.text.*", "java.time.*", "java.time.chrono.*", "java.time.format.*",
            "java.time.temporal.*", "java.time.zone.*", "java.util.*", "java.util.concurrent.*",
            "java.util.concurrent.atomic.*");

    /** Options passed to the compiler, before the class path. */
    public static final List<String> OPTIONS = List.of("--release", "21", "-proc:none", "-Xlint:deprecation", "-g");

    /** How long a snippet may run before it's abandoned. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private static final AtomicInteger COUNTER = new AtomicInteger();

    /** How a run ended. */
    public enum Status {
        /** Ran to the end. */
        OK,
        /** Compiled, but threw an exception (sometimes that's the point). */
        THREW,
        /** Didn't compile. */
        COMPILE_ERROR,
        /** Still running at the time limit, so abandoned. */
        TIMED_OUT,
        /** There is no compiler: running on a JRE rather than a JDK. */
        NO_COMPILER
    }

    /**
     * The result of compiling and running a snippet.
     *
     * @param warnings  compiler warnings, such as deprecations, as "line N: message"
     * @param errors    compiler errors as "line N: message", or the exception thrown
     * @param output    what the snippet printed
     * @param compileMs time spent compiling
     * @param runMs     time spent running
     */
    public record Result(Status status, List<String> warnings, List<String> errors, List<String> output,
            long compileMs, long runMs) {

        public Result {
            warnings = List.copyOf(warnings);
            errors = List.copyOf(errors);
            output = List.copyOf(output);
        }
    }

    private final String classPath;

    /** A runner that compiles against this application's class path. */
    public SnippetRunner() {
        this(defaultClassPath());
    }

    SnippetRunner(String classPath) {
        this.classPath = classPath;
    }

    /** Whether a compiler is available (a JDK, not just a JRE). */
    public static boolean isAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    /** The class path snippets compile against. */
    public String classPath() {
        return classPath;
    }

    /**
     * The whole source file compiled for {@code code}: imports, then a class
     * whose {@code run(PrintStream out)} method holds the snippet.
     */
    public static String compilationUnit(String className, String code) {
        List<String> lines = code.lines().toList();
        StringBuilder imports = new StringBuilder();
        int first = 0;
        while (first < lines.size() && (lines.get(first).isBlank() || lines.get(first).strip().startsWith("import "))) {
            imports.append(lines.get(first)).append('\n');
            first++;
        }
        StringBuilder unit = new StringBuilder();
        DEFAULT_IMPORTS.forEach(i -> unit.append("import ").append(i).append(";\n"));
        unit.append(imports);
        unit.append("\npublic final class ").append(className).append(" {\n");
        unit.append("    public static void run(PrintStream out) throws Exception {\n");
        for (String line : lines.subList(first, lines.size())) {
            unit.append(line).append('\n');
        }
        unit.append("    }\n}\n");
        return unit.toString();
    }

    /** Compiles and runs {@code code}, waiting at most {@link #DEFAULT_TIMEOUT}. */
    public Result run(String code) {
        return run(code, DEFAULT_TIMEOUT);
    }

    /** Compiles and runs {@code code}, waiting at most {@code timeout} for it to finish. */
    public Result run(String code, Duration timeout) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new Result(Status.NO_COMPILER, List.of(), List.of(
                    "No Java compiler is available. Run on a JDK (not a JRE) to run the examples."), List.of(), 0, 0);
        }
        String className = "Snippet" + COUNTER.incrementAndGet();
        String source = compilationUnit(className, code);
        int offset = lineOf(source, "public static void run(");   // snippet line 1 is the line after this one

        long started = System.nanoTime();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<String, ByteArrayOutputStream> classes = new ConcurrentHashMap<>();
        boolean compiled;
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.UK, StandardCharsets.UTF_8);
                JavaFileManager memory = new MemoryFileManager(standard, classes)) {
            List<String> options = new ArrayList<>(OPTIONS);
            options.addAll(List.of("-classpath", classPath));
            compiled = compiler.getTask(null, memory, diagnostics, options, null,
                    List.of(new SourceFile(className, source))).call();
        } catch (java.io.IOException e) {
            return new Result(Status.COMPILE_ERROR, List.of(), List.of("The compiler failed: " + e.getMessage()), List.of(), 0, 0);
        }
        long compileMs = (System.nanoTime() - started) / 1_000_000;

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String where = d.getLineNumber() > offset ? "line " + (d.getLineNumber() - offset) + ": " : "";
            String message = where + d.getMessage(Locale.UK).lines().findFirst().orElse("");
            (d.getKind() == Diagnostic.Kind.ERROR ? errors : warnings).add(message);
        }
        if (!compiled) {
            return new Result(Status.COMPILE_ERROR, warnings, errors, List.of(), compileMs, 0);
        }

        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(new LimitedOutputStream(printed, 200_000), true, StandardCharsets.UTF_8);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        MemoryClassLoader loader = new MemoryClassLoader(classes, SnippetRunner.class.getClassLoader());
        Thread thread = new Thread(() -> {
            try {
                Method run = loader.loadClass(className).getMethod("run", PrintStream.class);
                run.invoke(null, out);
            } catch (InvocationTargetException e) {
                thrown.set(e.getCause());
            } catch (ReflectiveOperationException | LinkageError e) {
                thrown.set(e);
            }
        }, "snippet " + className);
        thread.setDaemon(true);
        long runStarted = System.nanoTime();
        thread.start();
        try {
            thread.join(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long runMs = (System.nanoTime() - runStarted) / 1_000_000;
        out.flush();
        List<String> output = printed.toString(StandardCharsets.UTF_8).lines().toList();
        if (thread.isAlive()) {
            thread.interrupt();
            return new Result(Status.TIMED_OUT, warnings,
                    List.of("Still running after " + timeout.toSeconds() + " seconds, so it was abandoned."), output, compileMs, runMs);
        }
        Throwable t = thrown.get();
        if (t != null) {
            return new Result(Status.THREW, warnings, List.of(describe(t, className, offset)), output, compileMs, runMs);
        }
        return new Result(Status.OK, warnings, List.of(), output, compileMs, runMs);
    }

    /** "java.time.DateTimeException: Invalid date 'FEBRUARY 30' (line 2)". */
    private static String describe(Throwable t, String className, int offset) {
        String text = t.getClass().getName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getClassName().startsWith(className) && e.getLineNumber() > offset) {
                return text + " (line " + (e.getLineNumber() - offset) + ")";
            }
        }
        return text;
    }

    private static int lineOf(String source, String text) {
        List<String> lines = source.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(text)) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * The application's class path, plus the jars of libraries the lessons use
     * (Joda-Time), in case the plugin was loaded some other way.
     */
    static String defaultClassPath() {
        Set<String> entries = new LinkedHashSet<>();
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                entries.add(entry);
            }
        }
        for (Class<?> library : List.of(org.joda.time.DateTime.class)) {
            try {
                var source = library.getProtectionDomain().getCodeSource();
                if (source != null) {
                    entries.add(Path.of(source.getLocation().toURI()).toString());
                }
            } catch (URISyntaxException | SecurityException | IllegalArgumentException ignored) {
                // the class path alone will have to do
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    // ---- in-memory compilation ----------------------------------------------------------------

    private static final class SourceFile extends SimpleJavaFileObject {

        private final String source;

        SourceFile(String className, String source) {
            super(URI.create("string:///" + className + ".java"), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class ClassFile extends SimpleJavaFileObject {

        private final ByteArrayOutputStream bytes;

        ClassFile(String className, ByteArrayOutputStream bytes) {
            super(URI.create("bytes:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
            this.bytes = bytes;
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    private static final class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private final Map<String, ByteArrayOutputStream> classes;

        MemoryFileManager(JavaFileManager delegate, Map<String, ByteArrayOutputStream> classes) {
            super(delegate);
            this.classes = classes;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                FileObject sibling) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            classes.put(className, bytes);
            return new ClassFile(className, bytes);
        }
    }

    private static final class MemoryClassLoader extends ClassLoader {

        private final Map<String, ByteArrayOutputStream> classes;

        MemoryClassLoader(Map<String, ByteArrayOutputStream> classes, ClassLoader parent) {
            super(parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            ByteArrayOutputStream bytes = classes.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] b = bytes.toByteArray();
            return defineClass(name, b, 0, b.length);
        }
    }

    /** Stops collecting output past a limit, so a runaway loop can't fill memory. */
    private static final class LimitedOutputStream extends OutputStream {

        private final ByteArrayOutputStream target;
        private final int limit;

        LimitedOutputStream(ByteArrayOutputStream target, int limit) {
            this.target = target;
            this.limit = limit;
        }

        @Override
        public synchronized void write(int b) {
            if (target.size() < limit) {
                target.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            int room = limit - target.size();
            if (room > 0) {
                target.write(b, off, Math.min(len, room));
            }
        }
    }
}
