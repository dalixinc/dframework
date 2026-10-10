package com.dframework.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dframework.time.JavaSyntax.Kind;
import com.dframework.time.JavaSyntax.Span;

class JavaSyntaxTest {

    private static List<String> words(String code, Kind kind) {
        return JavaSyntax.spans(code).stream().filter(s -> s.kind() == kind)
                .map(s -> code.substring(s.start(), s.start() + s.length())).toList();
    }

    @Test
    void coloursKeywordsTypesStringsNumbersAndComments() {
        String code = "Date d = new Date(2026, Calendar.FEBRUARY, 30); // \"rolls\" over\nout.println(\"a \\\"b\\\"\");";
        assertThat(words(code, Kind.KEYWORD)).containsExactly("new");
        assertThat(words(code, Kind.TYPE)).containsExactly("Date", "Date", "Calendar");   // not FEBRUARY
        assertThat(words(code, Kind.NUMBER)).containsExactly("2026", "30");
        assertThat(words(code, Kind.COMMENT)).containsExactly("// \"rolls\" over");
        assertThat(words(code, Kind.STRING)).containsExactly("\"a \\\"b\\\"\"");
    }

    @Test
    void numbersKeepUnderscoresDecimalsAndExponents() {
        assertThat(words("long x = 86_400_000L; double g = 3.986004418e14;", Kind.NUMBER))
                .containsExactly("86_400_000L", "3.986004418e14");
    }

    @Test
    void anUnterminatedStringOrCommentStopsAtTheEnd() {
        List<Span> spans = JavaSyntax.spans("x = \"open\n/* never closed");
        assertThat(spans).extracting(Span::kind).containsExactly(Kind.STRING, Kind.COMMENT);
        assertThat(spans.get(1).start() + spans.get(1).length()).isEqualTo(25);
    }
}
