package com.dframework.dig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads dig's text output into records and a summary, for the Answers table
 * and the status line. Handles the normal layout (with ANSWER, AUTHORITY and
 * ADDITIONAL sections), +short, and +trace (where records are grouped by the
 * server that returned them).
 */
public final class DigOutput {

    /** One resource record. {@code ttl} is null when dig didn't print one (+short). */
    public record DnsRecord(String section, String name, Integer ttl, String dnsClass, String type, String data) {
    }

    /**
     * What happened overall. Fields are null when the output didn't say (for
     * example, +short prints no header).
     */
    public record Summary(String status, Integer queryTimeMs, String server, String protocol, Integer messageSize,
            int answers, int hops, List<String> problems) {
    }

    /** The parsed output. */
    public record Result(List<DnsRecord> records, Summary summary) {
    }

    private static final Set<String> CLASSES = Set.of("IN", "CH", "HS", "CS", "ANY");
    private static final Pattern SECTION = Pattern.compile(";; (QUESTION|ANSWER|AUTHORITY|ADDITIONAL) SECTION:");
    private static final Pattern STATUS = Pattern.compile("status: ([A-Z]+)");
    private static final Pattern QUERY_TIME = Pattern.compile(";; Query time: (\\d+) msec");
    private static final Pattern SERVER = Pattern.compile(";; SERVER: ([^#\\s]+)#\\d+\\([^)]*\\)(?: \\((\\w+)\\))?");
    private static final Pattern MSG_SIZE = Pattern.compile(";; MSG SIZE\\s+rcvd: (\\d+)");
    private static final Pattern RECEIVED = Pattern.compile(";; Received (\\d+) bytes from ([^#\\s]+)#\\d+\\(([^)]*)\\) in (\\d+) ms");

    private DigOutput() {
    }

    public static Result parse(List<String> lines, DigOptions options) {
        return options.has(DigOptions.Flag.SHORT) ? parseShort(lines, options) : parseFull(lines);
    }

    private static Result parseShort(List<String> lines, DigOptions options) {
        List<DnsRecord> records = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        String type = options.has(DigOptions.Flag.REVERSE) ? "PTR" : options.type();
        for (String line : lines) {
            String text = line.strip();
            if (text.isEmpty()) {
                continue;
            }
            if (text.startsWith(";;")) {
                problems.add(text.substring(2).strip());
                continue;
            }
            records.add(new DnsRecord("Answer", options.name(), null, null, type, text));
        }
        return new Result(records, new Summary(null, null, null, null, null, records.size(), 0, problems));
    }

    private static Result parseFull(List<String> lines) {
        List<DnsRecord> records = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<String[]> pendingTrace = new ArrayList<>();
        String section = null;
        String status = null;
        String server = null;
        String protocol = null;
        Integer queryTime = null;
        Integer size = null;
        int hops = 0;
        for (String line : lines) {
            String text = line.strip();
            if (text.isEmpty()) {
                continue;
            }
            Matcher m;
            if ((m = SECTION.matcher(text)).matches()) {
                section = switch (m.group(1)) {
                    case "QUESTION" -> null;   // the question lines start with ';' anyway
                    case "ANSWER" -> "Answer";
                    case "AUTHORITY" -> "Authority";
                    default -> "Additional";
                };
            } else if ((m = RECEIVED.matcher(text)).find()) {
                // +trace: the records just printed came from this server.
                hops++;
                String hop = "Hop " + hops + " · " + m.group(3) + " (" + m.group(4) + " ms)";
                for (String[] t : pendingTrace) {
                    records.add(toRecord(hop, t));
                }
                pendingTrace.clear();
            } else if (text.startsWith(";")) {
                if ((m = STATUS.matcher(text)).find()) {
                    status = m.group(1);
                } else if ((m = QUERY_TIME.matcher(text)).find()) {
                    queryTime = Integer.valueOf(m.group(1));
                } else if ((m = SERVER.matcher(text)).find()) {
                    server = m.group(1);
                    protocol = m.group(2);
                } else if ((m = MSG_SIZE.matcher(text)).find()) {
                    size = Integer.valueOf(m.group(1));
                } else if (text.contains("failed") || text.contains("could not") || text.contains("timed out")
                        || text.contains("couldn't") || text.contains("no servers")) {
                    problems.add(text.replaceFirst("^;+\\s*", ""));
                }
            } else {
                String[] t = text.split("\\s+", 5);
                if (t.length == 5 && t[1].matches("\\d+") && CLASSES.contains(t[2])) {
                    if (section == null) {
                        pendingTrace.add(t);   // +trace has no section headers
                    } else {
                        records.add(toRecord(section, t));
                    }
                } else {
                    problems.add(text);
                }
            }
        }
        for (String[] t : pendingTrace) {
            records.add(toRecord("Trace", t));
        }
        int answers = (int) records.stream().filter(r -> r.section().equals("Answer")).count();
        if (hops > 0) {
            answers = countFinalHop(records, hops);
        }
        return new Result(records, new Summary(status, queryTime, server, protocol, size, answers, hops, problems));
    }

    /** In a trace, the answers are the records from the last hop. */
    private static int countFinalHop(List<DnsRecord> records, int hops) {
        String prefix = "Hop " + hops + " ·";
        return (int) records.stream().filter(r -> r.section().startsWith(prefix)).count();
    }

    private static DnsRecord toRecord(String section, String[] t) {
        return new DnsRecord(section, t[0], Integer.valueOf(t[1]), t[2], t[3], t[4].replaceAll("\\s+", " "));
    }
}
