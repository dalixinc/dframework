package com.dframework.dig;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dframework.dig.DigOptions.Flag;
import com.dframework.dig.DigOutput.DnsRecord;

/** Parses real dig 9.18 output captured into src/test/resources. */
class DigOutputTest {

    private static List<String> fixture(String name) throws IOException {
        try (InputStream in = DigOutputTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static DigOptions options(String name, String type, Set<Flag> flags) {
        return new DigOptions(name, type, "", flags, 5);
    }

    @Test
    void mxRecordsWithSummary() throws IOException {
        DigOutput.Result result = DigOutput.parse(fixture("mx.txt"), options("gmail.com", "MX", Set.of()));

        assertThat(result.records()).hasSize(5).allSatisfy(r -> {
            assertThat(r.section()).isEqualTo("Answer");
            assertThat(r.name()).isEqualTo("gmail.com.");
            assertThat(r.type()).isEqualTo("MX");
            assertThat(r.dnsClass()).isEqualTo("IN");
        });
        assertThat(result.records().get(1).data()).isEqualTo("5 gmail-smtp-in.l.google.com.");
        assertThat(result.records().get(0).ttl()).isEqualTo(1877);

        DigOutput.Summary s = result.summary();
        assertThat(s.status()).isEqualTo("NOERROR");
        assertThat(s.answers()).isEqualTo(5);
        assertThat(s.queryTimeMs()).isEqualTo(6);
        assertThat(s.server()).isEqualTo("127.0.0.53");
        assertThat(s.protocol()).isEqualTo("UDP");
        assertThat(s.messageSize()).isEqualTo(161);
        assertThat(s.problems()).isEmpty();
    }

    @Test
    void nxdomainHasNoAnswersButAnAuthority() throws IOException {
        DigOutput.Result result = DigOutput.parse(fixture("nxdomain.txt"),
                options("dframework-no-such-name.zzzinvalidtld", "A", Set.of()));

        assertThat(result.summary().status()).isEqualTo("NXDOMAIN");
        assertThat(result.summary().answers()).isZero();
        assertThat(result.records()).extracting(DnsRecord::section).containsOnly("Authority");
        assertThat(result.records()).extracting(DnsRecord::type).contains("SOA");
    }

    @Test
    void reverseLookupFindsThePtrRecord() throws IOException {
        DigOutput.Result result = DigOutput.parse(fixture("reverse.txt"), options("8.8.8.8", "A", EnumSet.of(Flag.REVERSE)));

        assertThat(result.records()).singleElement().satisfies(r -> {
            assertThat(r.type()).isEqualTo("PTR");
            assertThat(r.data()).isEqualTo("dns.google.");
        });
    }

    @Test
    void shortOutputIsJustData() throws IOException {
        DigOutput.Result result = DigOutput.parse(fixture("short-mx.txt"), options("gmail.com", "MX", EnumSet.of(Flag.SHORT)));

        assertThat(result.records()).hasSize(5).allSatisfy(r -> {
            assertThat(r.name()).isEqualTo("gmail.com");
            assertThat(r.type()).isEqualTo("MX");
            assertThat(r.ttl()).isNull();
        });
        assertThat(result.summary().status()).isNull();
    }

    @Test
    void traceGroupsRecordsByTheServerThatReturnedThem() throws IOException {
        DigOutput.Result result = DigOutput.parse(fixture("trace.txt"), options("example.com", "A", EnumSet.of(Flag.TRACE)));
        DigOutput.Summary s = result.summary();

        assertThat(s.hops()).isGreaterThanOrEqualTo(3);
        assertThat(result.records().get(0).section()).startsWith("Hop 1 · 127.0.0.53");
        assertThat(result.records().get(0).name()).isEqualTo(".");
        List<DnsRecord> last = result.records().stream().filter(r -> r.section().startsWith("Hop " + s.hops() + " ")).toList();
        assertThat(last).extracting(DnsRecord::type).contains("A");
        assertThat(s.answers()).isEqualTo(last.size());
        // The real capture includes IPv6 "network unreachable" failures along the way.
        assertThat(s.problems()).anySatisfy(p -> assertThat(p).contains("network unreachable"));
    }
}
