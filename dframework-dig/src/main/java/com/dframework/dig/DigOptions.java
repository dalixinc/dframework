package com.dframework.dig;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything that decides which dig command runs.
 *
 * @param name           the name to look up, or the IP address for a reverse lookup
 * @param type           record type such as "A" or "MX" (ignored for reverse lookups)
 * @param server         DNS server to ask, or blank for the system resolver
 * @param flags          options switched on
 * @param timeoutSeconds seconds to wait for each reply
 */
public record DigOptions(String name, String type, String server, Set<DigOptions.Flag> flags, int timeoutSeconds) {

    /** dig's own default timeout; the command only mentions +time when it differs. */
    public static final int DEFAULT_TIMEOUT = 5;

    /** Record types offered in the user interface. */
    public static final List<String> TYPES = List.of(
            "A", "AAAA", "MX", "NS", "TXT", "CNAME", "SOA", "CAA", "SRV", "PTR", "DS", "DNSKEY", "ANY");

    /** On/off options, each with the dig arguments it adds. */
    public enum Flag {
        SHORT("Short", "Print just the answers (+short)", "+short"),
        TRACE("Trace", "Follow delegation from the root servers down (+trace)", "+trace"),
        DNSSEC("DNSSEC", "Include DNSSEC signature records (+dnssec)", "+dnssec"),
        TCP("TCP", "Query over TCP instead of UDP (+tcp)", "+tcp"),
        ANSWER_ONLY("Answer only", "Show only the answer section (+noall +answer)", "+noall", "+answer"),
        NO_RECURSION("No recursion", "Ask the server not to recurse (+norecurse)", "+norecurse"),
        REVERSE("Reverse (-x)", "Look up the name for an IP address (-x)", "-x");

        private final String label;
        private final String description;
        private final List<String> arguments;

        Flag(String label, String description, String... arguments) {
            this.label = label;
            this.description = description;
            this.arguments = List.of(arguments);
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public List<String> arguments() {
            return arguments;
        }
    }

    public DigOptions {
        name = Objects.requireNonNullElse(name, "").strip();
        type = Objects.requireNonNullElse(type, "A").strip();
        server = Objects.requireNonNullElse(server, "").strip();
        flags = flags.isEmpty() ? Collections.unmodifiableSet(EnumSet.noneOf(Flag.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(flags));
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("timeoutSeconds must be at least 1");
        }
    }

    public boolean has(Flag flag) {
        return flags.contains(flag);
    }
}
