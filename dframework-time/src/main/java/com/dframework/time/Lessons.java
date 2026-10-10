package com.dframework.time;

import java.util.List;

import com.dframework.time.Lesson.Era;
import com.dframework.time.Lesson.Snippet;

/**
 * The tour's content: Java's date and time APIs from java.util.Date to
 * java.time, then on to leap seconds, GPS time and relativity.
 *
 * <p>Every snippet is compiled and run live, and {@code LessonsTest} runs them
 * all, so the examples can't stop working before the talk.
 */
public final class Lessons {

    private Lessons() {
    }

    /** All lessons, in order. */
    public static List<Lesson> all() {
        return LESSONS;
    }

    private static final List<Lesson> LESSONS = List.of(

            // ---- 1996: java.util.Date ----------------------------------------------------------------

            new Lesson(Era.DATE, "Year 3926, month 11",
                    "java.util.Date arrived in JDK 1.0, in a hurry. Its constructor counts years from 1900 and "
                            + "months from 0, so the obvious way to write 10 October 2026 gives you November, "
                            + "nineteen centuries from now. The compiler warns you, at least: these methods were "
                            + "deprecated in 1997.",
                    new Snippet("The pitfall: java.util.Date (1996)", """
                            // 10 October 2026... or is it?
                            Date talk = new Date(2026, 10, 10);
                            out.println(talk);
                            out.println("getYear():  " + talk.getYear());
                            out.println("getMonth(): " + talk.getMonth());

                            // What you had to write instead
                            out.println(new Date(2026 - 1900, 10 - 1, 10));
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            LocalDate talk = LocalDate.of(2026, 10, 10);
                            out.println(talk);
                            out.println("Year:  " + talk.getYear());
                            out.println("Month: " + talk.getMonth() + " (" + talk.getMonthValue() + ")");
                            out.println("Day:   " + talk.getDayOfWeek());
                            """),
                    "Years mean years and months count from 1. Read the compiler's deprecation warnings."),

            new Lesson(Era.DATE, "A Date isn't a date",
                    "Despite its name, a Date is an instant: milliseconds since midnight UTC on 1 January 1970. "
                            + "Its toString() quietly uses this computer's time zone. In Britain the epoch shows as "
                            + "01:00 GMT, which is a contradiction: from 1968 to 1971 the UK stayed an hour ahead all "
                            + "year ('British Standard Time'), but the zone's name still says GMT.",
                    new Snippet("The pitfall: java.util.Date (1996)", """
                            Date epoch = new Date(0);        // the moment Java counts from
                            out.println(epoch);              // in this computer's zone, silently
                            out.println("Default zone: " + TimeZone.getDefault().getID());

                            SimpleDateFormat london = new SimpleDateFormat("d MMM yyyy HH:mm zzz (Z)", Locale.UK);
                            london.setTimeZone(TimeZone.getTimeZone("Europe/London"));
                            out.println("In London:    " + london.format(epoch));
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            Instant epoch = Instant.EPOCH;   // a moment: always shown in UTC
                            out.println(epoch);
                            out.println(epoch.atZone(ZoneId.of("Europe/London")));
                            out.println(epoch.atZone(ZoneId.of("America/New_York")));

                            LocalDate justADate = LocalDate.of(1970, 1, 1);   // a date: no time, no zone
                            out.println(justADate);
                            """),
                    "Instant is a moment; LocalDate is a date. Date was trying to be both, and neither."),

            new Lesson(Era.DATE, "The date that changed itself",
                    "Date is mutable. A class can keep a Date in a final field, hand it out from a getter, and "
                            + "still find it changed by someone else's code. Defensive copies everywhere were the "
                            + "only cure.",
                    new Snippet("The pitfall: java.util.Date (1996)", """
                            class Booking {
                                private final Date start;
                                Booking(Date start) { this.start = start; }
                                Date getStart() { return start; }      // final, so safe... right?
                            }

                            Booking talk = new Booking(new Date(126, 9, 10));
                            out.println("Booked:   " + talk.getStart());
                            talk.getStart().setTime(0);                // someone else's code
                            out.println("Now it's: " + talk.getStart());
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            record Booking(LocalDate start) { }

                            Booking talk = new Booking(LocalDate.of(2026, 10, 10));
                            LocalDate later = talk.start().plusDays(7);   // a new object
                            out.println("A week later: " + later);
                            out.println("Still booked: " + talk.start());   // can't be changed
                            """),
                    "java.time objects are immutable: every \"change\" returns a new value."),

            // ---- 1997: Calendar ----------------------------------------------------------------------

            new Lesson(Era.CALENDAR, "February 30th is March 2nd",
                    "JDK 1.1 moved the arithmetic into Calendar, licensed from IBM's Taligent. Months still count "
                            + "from 0, so there are constants such as Calendar.FEBRUARY (which is 1). And Calendar is "
                            + "lenient: an impossible date silently rolls over into a real one.",
                    new Snippet("The pitfall: Calendar (1997)", """
                            Calendar cal = new GregorianCalendar(2026, Calendar.FEBRUARY, 30);
                            out.println(cal.getTime());       // no error: it rolls over

                            cal.set(Calendar.MONTH, 12);      // the thirteenth month
                            out.println(cal.getTime());
                            out.println("Calendar.FEBRUARY = " + Calendar.FEBRUARY);
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            out.println(LocalDate.of(2026, Month.FEBRUARY, 28));
                            out.println(LocalDate.of(2026, 2, 30));   // there is no 30 February
                            """),
                    "An impossible date should be an error, not a surprise. java.time throws."),

            new Lesson(Era.CALENDAR, "SimpleDateFormat and threads",
                    "Formatters are expensive to make, so programs kept one in a static field and shared it. But "
                            + "SimpleDateFormat keeps its working state in fields, so threads sharing one tread on "
                            + "each other: wrong dates, and exceptions, depending on timing. A classic production "
                            + "bug, rarely reproducible on a developer's machine.",
                    new Snippet("The pitfall: SimpleDateFormat (1997)", """
                            SimpleDateFormat shared = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                            List<String> days = List.of("2026-01-15", "1999-12-31", "1970-01-01", "2038-01-19");
                            AtomicInteger wrong = new AtomicInteger(), crashed = new AtomicInteger();

                            ExecutorService pool = Executors.newFixedThreadPool(8);
                            for (int i = 0; i < 4000; i++) {
                                String day = days.get(i % days.size());
                                pool.execute(() -> {
                                    try {
                                        Date parsed = shared.parse(day);
                                        String back = new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(parsed);
                                        if (!back.equals(day)) wrong.incrementAndGet();
                                    } catch (Exception e) {
                                        crashed.incrementAndGet();
                                    }
                                });
                            }
                            pool.shutdown();
                            pool.awaitTermination(10, TimeUnit.SECONDS);
                            out.println("4,000 parses on 8 threads, one shared formatter");
                            out.println("Wrong dates: " + wrong);
                            out.println("Exceptions:  " + crashed);
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            DateTimeFormatter shared = DateTimeFormatter.ofPattern("uuuu-MM-dd");   // immutable
                            List<String> days = List.of("2026-01-15", "1999-12-31", "1970-01-01", "2038-01-19");
                            AtomicInteger wrong = new AtomicInteger(), crashed = new AtomicInteger();

                            ExecutorService pool = Executors.newFixedThreadPool(8);
                            for (int i = 0; i < 4000; i++) {
                                String day = days.get(i % days.size());
                                pool.execute(() -> {
                                    try {
                                        String back = LocalDate.parse(day, shared).format(shared);
                                        if (!back.equals(day)) wrong.incrementAndGet();
                                    } catch (Exception e) {
                                        crashed.incrementAndGet();
                                    }
                                });
                            }
                            pool.shutdown();
                            pool.awaitTermination(10, TimeUnit.SECONDS);
                            out.println("4,000 parses on 8 threads, one shared formatter");
                            out.println("Wrong dates: " + wrong);
                            out.println("Exceptions:  " + crashed);
                            """),
                    "DateTimeFormatter is immutable and thread-safe: share it freely."),

            new Lesson(Era.CALENDAR, "YYYY isn't yyyy",
                    "In a date pattern, yyyy is the year, but YYYY is the week-based year: the year that owns the "
                            + "week. For the last days of December it can be next year. This one went out in "
                            + "production apps on 29 December, and java.time kept the same letter, so it can still "
                            + "catch you.",
                    new Snippet("The pitfall: SimpleDateFormat (1997)", """
                            Date monday = new GregorianCalendar(2025, Calendar.DECEMBER, 29).getTime();
                            out.println("YYYY: " + new SimpleDateFormat("YYYY-MM-dd", Locale.UK).format(monday));
                            out.println("yyyy: " + new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(monday));
                            """),
                    new Snippet("The fix: use the ISO formats (2014)", """
                            LocalDate monday = LocalDate.of(2025, 12, 29);
                            // java.time kept the letter: YYYY is still the week-based year
                            out.println("YYYY: " + monday.format(DateTimeFormatter.ofPattern("YYYY-MM-dd", Locale.UK)));
                            out.println("uuuu: " + monday.format(DateTimeFormatter.ofPattern("uuuu-MM-dd")));
                            // Safest: no pattern to get wrong at all
                            out.println("ISO:  " + monday.format(DateTimeFormatter.ISO_LOCAL_DATE));
                            out.println("Week: " + monday.format(DateTimeFormatter.ISO_WEEK_DATE));
                            """),
                    "Prefer the predefined ISO formatters. If you write a pattern, check every letter."),

            new Lesson(Era.CALENDAR, "Same date, ten days apart",
                    "Shakespeare and Cervantes both died on 23 April 1616, but not on the same day. Spain had "
                            + "adopted Pope Gregory's calendar in 1582; England kept the Julian calendar until 1752, "
                            + "when 2 September was followed by 14 September. GregorianCalendar can model the "
                            + "switch; java.time uses the Gregorian calendar for all of history and maps others "
                            + "through a day count.",
                    new Snippet("History: GregorianCalendar (1997)", """
                            GregorianCalendar spain = new GregorianCalendar(1616, Calendar.APRIL, 23);

                            GregorianCalendar england = new GregorianCalendar();
                            england.setGregorianChange(new Date(Long.MAX_VALUE));   // Julian, always
                            england.clear();
                            england.set(1616, Calendar.APRIL, 23);

                            long days = (england.getTimeInMillis() - spain.getTimeInMillis()) / 86_400_000;
                            out.println("Cervantes:   " + spain.getTime());
                            out.println("Shakespeare: " + england.getTime());
                            out.println("Days apart:  " + days);
                            """),
                    new Snippet("History: java.time (2014)", """
                            // java.time is Gregorian all the way back. To read a Julian date, convert it
                            // to a Julian Day Number, the astronomers' day count, and back.
                            int y = 1616, m = 4, d = 23;
                            int a = (14 - m) / 12, yy = y + 4800 - a, mm = m + 12 * a - 3;
                            long julianDay = d + (153 * mm + 2) / 5 + 365L * yy + yy / 4 - 32083;

                            LocalDate shakespeare = LocalDate.EPOCH.with(JulianFields.JULIAN_DAY, julianDay);
                            LocalDate cervantes = LocalDate.of(1616, 4, 23);
                            out.println("Cervantes:   " + cervantes + ", a " + cervantes.getDayOfWeek());
                            out.println("Shakespeare: " + shakespeare + ", a " + shakespeare.getDayOfWeek());
                            out.println("Days apart:  " + ChronoUnit.DAYS.between(cervantes, shakespeare));
                            """),
                    "A date means nothing without its calendar. java.time is Gregorian (ISO) unless you say otherwise."),

            // ---- 2005: Joda-Time ---------------------------------------------------------------------

            new Lesson(Era.JODA, "Joda-Time: the fix that came first",
                    "Fed up with Date and Calendar, Stephen Colebourne wrote Joda-Time: months from 1, immutable "
                            + "values, real time zones, readable code. It became the de facto standard, and he then "
                            + "led JSR-310 to put a redesigned version in the JDK. Joda-Time's own site now says: "
                            + "please move to java.time.",
                    new Snippet("Before: Calendar (1997)", """
                            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));
                            cal.clear();
                            cal.set(2026, Calendar.OCTOBER, 10, 9, 30);   // October is 9
                            cal.add(Calendar.MONTH, 1);                    // changes cal itself

                            SimpleDateFormat f = new SimpleDateFormat("EEE d MMM yyyy HH:mm Z", Locale.UK);
                            f.setTimeZone(cal.getTimeZone());
                            out.println(f.format(cal.getTime()));
                            """),
                    new Snippet("After: Joda-Time (2005)", """
                            import org.joda.time.DateTime;
                            import org.joda.time.DateTimeZone;

                            DateTime talk = new DateTime(2026, 10, 10, 9, 30, DateTimeZone.forID("Europe/London"));
                            DateTime nextMonth = talk.plusMonths(1);   // a new object

                            out.println(talk.toString("EEE d MMM yyyy HH:mm Z", Locale.UK));
                            out.println(nextMonth.toString("EEE d MMM yyyy HH:mm Z", Locale.UK));
                            """),
                    "Joda-Time proved the design. If you still use it, java.time is the way forward."),

            new Lesson(Era.JODA, "The hour that doesn't exist",
                    "When the clocks go forward, an hour of wall-clock time is skipped: in Britain on 29 March "
                            + "2026 there is no 01:30. Calendar quietly moves it. Joda-Time refuses with an "
                            + "exception. java.time lets you see the gap and choose: of() moves the time forward, "
                            + "as documented, and ofStrict() refuses.",
                    new Snippet("The pitfall: Calendar (1997)", """
                            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));
                            cal.clear();
                            cal.set(2026, Calendar.MARCH, 29, 1, 30);   // clocks go forward at 01:00

                            SimpleDateFormat f = new SimpleDateFormat("d MMM HH:mm zzz", Locale.UK);
                            f.setTimeZone(cal.getTimeZone());
                            out.println("Calendar:   " + f.format(cal.getTime()));   // silently 02:30
                            """),
                    new Snippet("Joda-Time (2005) and java.time (2014)", """
                            import org.joda.time.DateTime;
                            import org.joda.time.DateTimeZone;

                            try {
                                new DateTime(2026, 3, 29, 1, 30, DateTimeZone.forID("Europe/London"));
                            } catch (IllegalArgumentException e) {
                                out.println("Joda-Time:  " + e.getMessage());
                            }

                            ZoneId london = ZoneId.of("Europe/London");
                            LocalDateTime wallClock = LocalDateTime.of(2026, 3, 29, 1, 30);
                            out.println("Valid offsets: " + london.getRules().getValidOffsets(wallClock));
                            out.println("java.time:  " + ZonedDateTime.of(wallClock, london));
                            out.println("Strict:     " + ZonedDateTime.ofStrict(wallClock, ZoneOffset.UTC, london));
                            """),
                    "Daylight saving makes some local times happen never, and some twice. Decide what you want."),

            // ---- 2014: java.time ---------------------------------------------------------------------

            new Lesson(Era.JAVA_TIME, "A day isn't 24 hours",
                    "On the night the clocks go back, the day is 25 hours long. Code that adds 86,400,000 "
                            + "milliseconds for \"tomorrow, same time\" is an hour out. java.time separates the two "
                            + "questions: plusDays() means the same time tomorrow, plusHours(24) means 24 hours from "
                            + "now.",
                    new Snippet("The pitfall: Date arithmetic (1996)", """
                            // UK clocks go back at 02:00 on Sunday 25 October 2026
                            TimeZone london = TimeZone.getTimeZone("Europe/London");
                            Calendar cal = new GregorianCalendar(london);
                            cal.clear();
                            cal.set(2026, Calendar.OCTOBER, 24, 12, 0);
                            Date saturdayNoon = cal.getTime();
                            Date tomorrow = new Date(saturdayNoon.getTime() + 24 * 60 * 60 * 1000);

                            SimpleDateFormat f = new SimpleDateFormat("EEE d MMM HH:mm zzz", Locale.UK);
                            f.setTimeZone(london);
                            out.println("Saturday noon:   " + f.format(saturdayNoon));
                            out.println("+ 86,400,000 ms: " + f.format(tomorrow));   // not noon
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            ZonedDateTime saturdayNoon = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0,
                                    ZoneId.of("Europe/London"));
                            out.println("Saturday noon: " + saturdayNoon);
                            out.println("plusDays(1):   " + saturdayNoon.plusDays(1));     // noon again
                            out.println("plusHours(24): " + saturdayNoon.plusHours(24));   // 11:00
                            Duration sunday = Duration.between(saturdayNoon, saturdayNoon.plusDays(1));
                            out.println("That \\"day\\" lasted " + sunday.toHours() + " hours");
                            """),
                    "A day is a calendar idea; 24 hours is a duration. Say which you mean."),

            new Lesson(Era.JAVA_TIME, "31 January plus a month",
                    "There is no 31 February, so adding a month to 31 January gives 28 February. That's "
                            + "reasonable. But keep adding a month at a time and the 28th sticks: every month "
                            + "after ends up on the 28th. Count from the original date each time, or use an adjuster "
                            + "to say what you mean.",
                    new Snippet("The pitfall: Calendar (1997)", """
                            Calendar cal = new GregorianCalendar(2026, Calendar.JANUARY, 31);
                            SimpleDateFormat f = new SimpleDateFormat("d MMM yyyy", Locale.UK);
                            for (int i = 0; i < 3; i++) {
                                cal.add(Calendar.MONTH, 1);        // each step from the last
                                out.println(f.format(cal.getTime()));
                            }
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            LocalDate start = LocalDate.of(2026, 1, 31);
                            for (int i = 1; i <= 3; i++) {
                                out.println(start.plusMonths(i));  // each step from the start
                            }
                            out.println("End of next month: "
                                    + start.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
                            """),
                    "Month arithmetic clamps to the month's end. Count from a fixed start, or use TemporalAdjusters."),

            new Lesson(Era.JAVA_TIME, "How old are you?",
                    "The quick way to work out an age is to count the days and divide by 365. It goes wrong in "
                            + "the days before a birthday, because leap days add up. A Period counts years, months "
                            + "and days the way people do.",
                    new Snippet("The pitfall: milliseconds (1996)", """
                            Calendar born = new GregorianCalendar(2000, Calendar.MARCH, 10);
                            Calendar today = new GregorianCalendar(2026, Calendar.MARCH, 9);   // the day before
                            long days = (today.getTimeInMillis() - born.getTimeInMillis()) / 86_400_000L;
                            out.println("Days alive:       " + days);
                            out.println("Age (days / 365): " + days / 365);   // already 26?
                            """),
                    new Snippet("The fix: Period (2014)", """
                            LocalDate born = LocalDate.of(2000, 3, 10);
                            LocalDate today = LocalDate.of(2026, 3, 9);
                            Period age = Period.between(born, today);
                            out.println("Days alive: " + ChronoUnit.DAYS.between(born, today));
                            out.println("Age: " + age.getYears() + " years, " + age.getMonths() + " months, "
                                    + age.getDays() + " days");
                            """),
                    "Period is years, months and days; Duration is seconds. Ages and anniversaries are Periods."),

            new Lesson(Era.JAVA_TIME, "Nine o'clock where?",
                    "A meeting at 09:00 on 2 November is a wall-clock time. It only becomes a moment once you "
                            + "know where. Date has no way to say; it uses this computer's zone. java.time has a "
                            + "type for each idea: LocalDateTime for the wall clock, ZonedDateTime for a place, "
                            + "Instant for the moment.",
                    new Snippet("The pitfall: java.util.Date (1996)", """
                            // "09:00 on 2 November 2026". Where?
                            Date meeting = new Date(126, 10, 2, 9, 0);   // this computer's zone, silently
                            out.println(meeting);
                            out.println(meeting.getTime() + " ms since 1970");
                            """),
                    new Snippet("The fix: java.time (2014)", """
                            LocalDateTime nineAm = LocalDateTime.of(2026, 11, 2, 9, 0);   // wall clock, no zone
                            for (String zone : List.of("Europe/London", "America/New_York", "Asia/Tokyo")) {
                                ZonedDateTime there = nineAm.atZone(ZoneId.of(zone));
                                out.printf("%-17s %s = %s%n", zone, there.toLocalTime(), there.toInstant());
                            }
                            ZonedDateTime london = nineAm.atZone(ZoneId.of("Europe/London"));
                            out.println("London's 09:00 in Tokyo: "
                                    + london.withZoneSameInstant(ZoneId.of("Asia/Tokyo")).toLocalTime());
                            """),
                    "Store a future event as a local time plus a zone. Store a past event as an Instant."),

            new Lesson(Era.JAVA_TIME, "Testing tomorrow, today",
                    "Code that reads the system clock can only be tested on the day it's run. Calendar adds "
                            + "its own trap: Sunday is day 1 and Saturday day 7. java.time takes a Clock, so a test "
                            + "can fix the time to any day it likes, and its days of the week are an enum.",
                    new Snippet("The pitfall: Calendar (1997)", """
                            int day = new GregorianCalendar().get(Calendar.DAY_OF_WEEK);
                            out.println("Today is day " + day
                                    + " (Sunday = " + Calendar.SUNDAY + ", Saturday = " + Calendar.SATURDAY + ")");
                            boolean weekend = day >= Calendar.SATURDAY;   // forgets Sunday is 1
                            out.println("Weekend? " + weekend + ". Is that right on a Sunday? Wait and see.");
                            """),
                    new Snippet("The fix: Clock (2014)", """
                            Clock sunday = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneId.of("Europe/London"));
                            DayOfWeek day = LocalDate.now(sunday).getDayOfWeek();
                            boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                            out.println("With a fixed clock it's always " + day + ". Weekend? " + weekend);
                            out.println("A week later: " + LocalDate.now(Clock.offset(sunday, Duration.ofDays(7))));
                            """),
                    "Pass a Clock into code that needs the time, and tests can choose the day."),

            // ---- Beyond the calendar ------------------------------------------------------------------

            new Lesson(Era.BEYOND, "23:59:60",
                    "The Earth's spin is slowing, irregularly, so atomic time and solar time drift apart. Since "
                            + "1972, 27 leap seconds have been added to UTC, as 23:59:60. Java pretends they don't "
                            + "exist: its time-scale spreads each one over the last moments of the day. You can "
                            + "still parse one, and ask whether you did.",
                    new Snippet("The pitfall: SimpleDateFormat (1997)", """
                            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
                            f.setTimeZone(TimeZone.getTimeZone("UTC"));
                            Date leap = f.parse("2016-12-31 23:59:60");   // the most recent leap second
                            out.println(f.format(leap));                   // lenient: next year already
                            """),
                    new Snippet("java.time (2014)", """
                            TemporalAccessor parsed = DateTimeFormatter.ISO_INSTANT.parse("2016-12-31T23:59:60Z");
                            out.println("As an Instant: " + Instant.from(parsed));
                            out.println("Leap second?   " + parsed.query(DateTimeFormatter.parsedLeapSecond()));
                            try {
                                Instant.parse("2016-12-31T23:59:61Z");
                            } catch (DateTimeException e) {
                                out.println("23:59:61:      " + e.getMessage());
                            }
                            """),
                    "UTC has leap seconds; Java's time-scale smooths them away. Precise timing needs another clock."),

            new Lesson(Era.BEYOND, "GPS time: 18 seconds ahead",
                    "GPS keeps its own time-scale. It started on 6 January 1980 and never adds leap seconds, so "
                            + "it is now 18 seconds ahead of UTC. Satellites broadcast the time as a week number "
                            + "and seconds into the week, and the original signal had only 10 bits for the week, "
                            + "so the counter rolls back to zero every 1,024 weeks, catching out older receivers.",
                    new Snippet("The pitfall: forgetting leap seconds", """
                            Instant gpsEpoch = Instant.parse("1980-01-06T00:00:00Z");
                            Instant now = Instant.parse("2026-10-10T12:00:00Z");   // fixed, so it's repeatable
                            long naive = Duration.between(gpsEpoch, now).getSeconds();
                            out.println("GPS seconds (naive): " + naive);
                            out.println("Seconds into the week: " + naive % 604_800);
                            out.println("...18 seconds behind what the satellites are broadcasting");
                            """),
                    new Snippet("The fix: count the leap seconds", """
                            Instant gpsEpoch = Instant.parse("1980-01-06T00:00:00Z");
                            Instant now = Instant.parse("2026-10-10T12:00:00Z");
                            long leapSeconds = 18;   // GPS minus UTC, since 1 January 2017
                            long gps = Duration.between(gpsEpoch, now).getSeconds() + leapSeconds;
                            long week = gps / 604_800;
                            out.println("GPS week " + week + ", second " + gps % 604_800);
                            out.println("Sent in 10 bits as week " + week % 1024);
                            for (int n = 1; n <= 3; n++) {
                                out.println("Week counter back to 0: " + LocalDate.of(1980, 1, 6).plusWeeks(1024L * n));
                            }
                            """),
                    "Every clock belongs to a time-scale. Converting between them is a lookup, not arithmetic."),

            new Lesson(Era.BEYOND, "Einstein's correction",
                    "A GPS receiver works out where it is from how long the satellites' signals took to arrive, "
                            + "so the satellites' atomic clocks must agree with the ground to a few nanoseconds. "
                            + "Relativity says they can't. Moving fast, they tick slower (special relativity); "
                            + "higher up, in weaker gravity, they tick faster (general relativity). The net effect "
                            + "is that they gain about 38 microseconds a day, so the clocks are deliberately set "
                            + "slow before launch.",
                    new Snippet("The problem: satellite clocks run fast", """
                            double c = 299_792_458.0;        // speed of light, m/s
                            double gm = 3.986004418e14;      // Earth's gravitational parameter, m³/s²
                            double earth = 6_371_000;        // Earth's radius, m
                            double orbit = 26_561_750;       // GPS orbit radius, m (about 20,200 km up)
                            double speed = Math.sqrt(gm / orbit);
                            double day = 86_400;

                            double special = -speed * speed / (2 * c * c) * day;            // moving: slower
                            double general = gm / (c * c) * (1 / earth - 1 / orbit) * day;   // higher: faster
                            out.printf("Orbital speed:      %,.0f m/s%n", speed);
                            out.printf("Special relativity: %+.1f µs a day%n", special * 1e6);
                            out.printf("General relativity: %+.1f µs a day%n", general * 1e6);
                            out.printf("Net:                %+.1f µs a day%n", (special + general) * 1e6);
                            out.printf("Uncorrected, ranges drift by about %.0f km a day%n", (special + general) * c / 1000);
                            """),
                    new Snippet("The fix: set the clocks slow before launch", """
                            double nominal = 10.23e6;          // GPS clock frequency, Hz
                            double offset = 4.4647e-10;        // the relativistic rate, as a fraction (IS-GPS-200)
                            double setOnGround = nominal * (1 - offset);
                            out.printf("Set on the ground: %,.5f Hz%n", setOnGround);
                            out.printf("Seen from Earth:   %,.5f Hz%n", setOnGround * (1 + offset));

                            Duration perDay = Duration.ofNanos(Math.round(offset * 86_400 * 1e9));
                            out.println("Correction: " + perDay.toNanos() / 1000.0 + " µs a day, "
                                    + perDay.multipliedBy(365).toNanos() / 1e6 + " ms a year");
                            """),
                    "Even time itself isn't absolute: GPS works because engineers believed Einstein."));
}
