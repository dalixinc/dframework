package com.dframework.time;

import java.util.Objects;

/**
 * One step of the tour: a story, the old way and the better way as runnable
 * snippets, and what to take away.
 *
 * @param era      where it sits in the history
 * @param title    a short title, such as "February 30th is March 2nd"
 * @param story    a few sentences setting the scene
 * @param before   the pitfall (or the problem)
 * @param after    the fix (or the explanation)
 * @param takeaway one line to remember
 */
public record Lesson(Era era, String title, String story, Snippet before, Snippet after, String takeaway) {

    public Lesson {
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(story, "story");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(takeaway, "takeaway");
    }

    /**
     * A runnable piece of code with a heading.
     *
     * @param heading what it shows, such as "The pitfall: java.util.Date"
     * @param code    the body of a method {@code run(PrintStream out)}, optionally starting with imports
     */
    public record Snippet(String heading, String code) {

        public Snippet {
            Objects.requireNonNull(heading, "heading");
            Objects.requireNonNull(code, "code");
        }
    }

    /** The eras of the tour, in order. */
    public enum Era {
        DATE("1996", "java.util.Date", "JDK 1.0: one class for every kind of time"),
        CALENDAR("1997", "Calendar", "JDK 1.1: Calendar, GregorianCalendar and SimpleDateFormat, bought in from IBM"),
        JODA("2005", "Joda-Time", "Stephen Colebourne's library: what Java should have had"),
        JAVA_TIME("2014", "java.time", "Java 8, JSR-310: Joda-Time's ideas, redesigned, in the JDK"),
        BEYOND("1980 →", "GPS and Einstein", "Beyond the calendar: leap seconds, GPS time and relativity");

        private final String year;
        private final String label;
        private final String summary;

        Era(String year, String label, String summary) {
            this.year = year;
            this.label = label;
            this.summary = summary;
        }

        public String year() {
            return year;
        }

        public String label() {
            return label;
        }

        public String summary() {
            return summary;
        }
    }
}
