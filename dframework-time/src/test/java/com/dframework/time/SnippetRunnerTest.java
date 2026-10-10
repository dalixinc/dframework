package com.dframework.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.dframework.time.SnippetRunner.Result;
import com.dframework.time.SnippetRunner.Status;

class SnippetRunnerTest {

    private final SnippetRunner runner = new SnippetRunner();

    @Test
    void runsASnippetAndCapturesWhatItPrints() {
        Result r = runner.run("out.println(LocalDate.of(2026, 10, 10).getDayOfWeek());");
        assertThat(r.status()).isEqualTo(Status.OK);
        assertThat(r.output()).containsExactly("SATURDAY");
    }

    @Test
    void reportsCompileErrorsWithSnippetLineNumbers() {
        Result r = runner.run("out.println(1);\nint x = \"two\";");
        assertThat(r.status()).isEqualTo(Status.COMPILE_ERROR);
        assertThat(r.errors()).singleElement().asString().startsWith("line 2: ").contains("incompatible types");
    }

    @Test
    void reportsDeprecationWarnings() {
        Result r = runner.run("out.println(new Date(126, 9, 10).getYear());");
        assertThat(r.status()).isEqualTo(Status.OK);
        assertThat(r.warnings()).anyMatch(w -> w.startsWith("line 1: ") && w.contains("deprecated"));
        assertThat(r.output()).containsExactly("126");
    }

    @Test
    void anExceptionIsReportedWithItsLineAndTheOutputBeforeIt() {
        Result r = runner.run("out.println(\"first\");\nout.println(LocalDate.of(2026, 2, 30));");
        assertThat(r.status()).isEqualTo(Status.THREW);
        assertThat(r.output()).containsExactly("first");
        assertThat(r.errors()).containsExactly("java.time.DateTimeException: Invalid date 'FEBRUARY 30' (line 2)");
    }

    @Test
    void leadingImportsBecomeImportsAndOverrideTheDefaults() {
        String code = "import org.joda.time.DateTime;\n\nout.println(new DateTime(2026, 10, 10, 0, 0).getMonthOfYear());";
        assertThat(SnippetRunner.compilationUnit("X", code)).contains("import org.joda.time.DateTime;\n")
                .contains("public static void run(PrintStream out)");
        Result r = runner.run(code);
        assertThat(r.status()).as(r.errors().toString()).isEqualTo(Status.OK);
        assertThat(r.output()).containsExactly("10");
    }

    @Test
    void aSnippetThatNeverEndsIsAbandoned() {
        Result r = runner.run("out.println(\"spinning\");\nwhile (!Thread.currentThread().isInterrupted()) { Thread.onSpinWait(); }", Duration.ofMillis(300));
        assertThat(r.status()).isEqualTo(Status.TIMED_OUT);
        assertThat(r.output()).containsExactly("spinning");
    }
}
