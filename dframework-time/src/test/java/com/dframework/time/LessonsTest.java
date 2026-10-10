package com.dframework.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import com.dframework.time.SnippetRunner.Result;
import com.dframework.time.SnippetRunner.Status;

/**
 * Runs every lesson's code, as the presenter would, and checks it still shows
 * what the lesson says it shows. If a JDK or Joda-Time update changes a
 * result, this fails before the talk does.
 */
class LessonsTest {

    private static final SnippetRunner RUNNER = new SnippetRunner();

    /** Text each snippet's output (or error) must contain, by lesson title: before, then after. */
    private static final Map<String, List<List<String>>> EXPECTED = Map.ofEntries(
            Map.entry("Year 3926, month 11", List.of(List.of("3926", "getMonth(): 10"), List.of("OCTOBER (10)", "SATURDAY"))),
            Map.entry("A Date isn't a date", List.of(List.of("1 Jan 1970 01:00 GMT (+0100)"),
                    List.of("1970-01-01T00:00:00Z", "1969-12-31T19:00-05:00[America/New_York]"))),
            Map.entry("The date that changed itself", List.of(List.of("Now it's: Thu Jan 01"), List.of("Still booked: 2026-10-10"))),
            Map.entry("February 30th is March 2nd", List.of(List.of("Mar 02", "Jan 02", "2027", "Calendar.FEBRUARY = 1"),
                    List.of("2026-02-28", "Invalid date 'FEBRUARY 30'"))),
            Map.entry("SimpleDateFormat and threads", List.of(List.of("4,000 parses"),
                    List.of("Wrong dates: 0", "Exceptions:  0"))),
            Map.entry("YYYY isn't yyyy", List.of(List.of("YYYY: 2026-12-29", "yyyy: 2025-12-29"),
                    List.of("YYYY: 2026-12-29", "uuuu: 2025-12-29", "ISO:  2025-12-29", "2026-W01-1"))),
            Map.entry("Same date, ten days apart", List.of(List.of("Tue May 03", "Days apart:  10"),
                    List.of("1616-05-03, a TUESDAY", "1616-04-23, a SATURDAY", "Days apart:  10"))),
            Map.entry("Joda-Time: the fix that came first", List.of(List.of("Tue 10 Nov 2026 09:30 +0000"),
                    List.of("Sat 10 Oct 2026 09:30 +0100", "Tue 10 Nov 2026 09:30 +0000"))),
            Map.entry("The hour that doesn't exist", List.of(List.of("29 Mar 02:30 BST"),
                    List.of("daylight savings time 'gap'", "Valid offsets: []", "2026-03-29T02:30+01:00", "does not exist"))),
            Map.entry("A day isn't 24 hours", List.of(List.of("Sun 25 Oct 11:00 GMT"),
                    List.of("plusDays(1):   2026-10-25T12:00", "plusHours(24): 2026-10-25T11:00", "lasted 25 hours"))),
            Map.entry("31 January plus a month", List.of(List.of("28 Feb 2026", "28 Mar 2026", "28 Apr 2026"),
                    List.of("2026-03-31", "2026-04-30", "End of next month: 2026-02-28"))),
            Map.entry("How old are you?", List.of(List.of("Days alive:       9495", "Age (days / 365): 26"),
                    List.of("25 years, 11 months, 27 days"))),
            Map.entry("Nine o'clock where?", List.of(List.of("ms since 1970"),
                    List.of("America/New_York  09:00 = 2026-11-02T14:00:00Z", "in Tokyo: 18:00"))),
            Map.entry("Testing tomorrow, today", List.of(List.of("Sunday = 1, Saturday = 7"),
                    List.of("always SUNDAY. Weekend? true", "A week later: 2026-10-18"))),
            Map.entry("23:59:60", List.of(List.of("2017-01-01 00:00:00"),
                    List.of("2016-12-31T23:59:59Z", "Leap second?   true", "could not be parsed"))),
            Map.entry("GPS time: 18 seconds ahead", List.of(List.of("Seconds into the week: 561600"),
                    List.of("GPS week 2439, second 561618", "week 391", "1999-08-22", "2019-04-07", "2038-11-21"))),
            Map.entry("Einstein's correction", List.of(
                    List.of("-7.2 µs", "+45.7 µs", "+38.5 µs", "12 km a day"),
                    List.of("10,229,999.99543 Hz", "10,230,000.00000 Hz", "38.575 µs a day"))));

    static Stream<Arguments> snippets() {
        return Lessons.all().stream().flatMap(l -> Stream.of(
                Arguments.of(l.title(), 0, l.before()), Arguments.of(l.title(), 1, l.after())));
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @MethodSource("snippets")
    void everySnippetRunsAndShowsWhatItsLessonSays(String title, int side, Lesson.Snippet snippet) {
        Result result = RUNNER.run(snippet.code());

        assertThat(result.status()).as(result.errors().toString()).isIn(Status.OK, Status.THREW);
        String everything = String.join("\n", result.output()) + "\n" + String.join("\n", result.errors());
        assertThat(EXPECTED).as("expectations for " + title).containsKey(title);
        for (String expected : EXPECTED.get(title).get(side)) {
            assertThat(everything).as(title + " [" + side + "]").contains(expected);
        }
    }

    @Test
    void lessonsAreInEraOrderWithUniqueTitles() {
        List<Lesson> lessons = Lessons.all();
        for (int i = 1; i < lessons.size(); i++) {
            assertThat(lessons.get(i).era().ordinal()).isGreaterThanOrEqualTo(lessons.get(i - 1).era().ordinal());
        }
        assertThat(lessons.stream().map(Lesson::title).distinct()).hasSameSizeAs(lessons);
        assertThat(EXPECTED).hasSameSizeAs(lessons);
    }
}
