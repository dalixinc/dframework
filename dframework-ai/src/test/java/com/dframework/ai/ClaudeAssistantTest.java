package com.dframework.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.dframework.swing.ai.AiAnswer;
import com.dframework.swing.ai.AiAssistant;
import com.dframework.swing.ai.AiException;
import com.dframework.swing.ai.AiPrompt;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Runs the real SDK code path against a local fake of the Messages API, so no
 * API key, network or money is needed.
 */
class ClaudeAssistantTest {

    private static final AiPrompt PROMPT = new AiPrompt("Explain DNS.", "Explain this dig result.");

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> betaHeader = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Serves one canned response to POST /v1/messages and records what was sent. */
    private ClaudeAssistant assistantAgainst(int status, String contentType, String body, long delayBetweenEventsMs)
            throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> respond(exchange, status, contentType, body, delayBetweenEventsMs));
        server.start();
        var client = AnthropicOkHttpClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("test-key")
                .maxRetries(0)
                .build();
        return new ClaudeAssistant(client, "claude-opus-5-5");
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body, long delayMs) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        betaHeader.set(exchange.getRequestHeaders().getFirst("anthropic-beta"));
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, 0);
        try (OutputStream out = exchange.getResponseBody()) {
            for (String event : body.split("(?<=\\n\\n)")) {
                out.write(event.getBytes(StandardCharsets.UTF_8));
                out.flush();
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        } catch (IOException clientWentAway) {
            // expected when the request is cancelled
        }
    }

    private static String sse(String type, String json) {
        return "event: " + type + "\ndata: " + json + "\n\n";
    }

    private static String streamOf(String stopReason, String... texts) {
        StringBuilder s = new StringBuilder();
        s.append(sse("message_start", """
                {"type":"message_start","message":{"id":"msg_test","type":"message","role":"assistant",\
                "model":"claude-opus-5-5","content":[],"stop_reason":null,"stop_sequence":null,\
                "usage":{"input_tokens":321,"output_tokens":1}}}"""));
        s.append(sse("content_block_start", """
                {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}"""));
        for (String text : texts) {
            s.append(sse("content_block_delta", "{\"type\":\"content_block_delta\",\"index\":0,"
                    + "\"delta\":{\"type\":\"text_delta\",\"text\":\"" + text + "\"}}"));
        }
        s.append(sse("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":0}"));
        s.append(sse("message_delta", "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"" + stopReason
                + "\",\"stop_sequence\":null},\"usage\":{\"output_tokens\":42}}"));
        s.append(sse("message_stop", "{\"type\":\"message_stop\"}"));
        return s.toString();
    }

    @Test
    void streamsTheAnswerAndReportsUsage() throws Exception {
        ClaudeAssistant claude = assistantAgainst(200, "text/event-stream",
                streamOf("end_turn", "gmail.com has ", "five mail servers."), 0);
        List<String> pieces = new ArrayList<>();

        AiAnswer answer = claude.ask(PROMPT, pieces::add);

        assertThat(pieces).containsExactly("gmail.com has ", "five mail servers.");
        assertThat(answer.text()).isEqualTo("gmail.com has five mail servers.");
        assertThat(answer.model()).isEqualTo("claude-opus-5-5");
        assertThat(answer.inputTokens()).isEqualTo(321);
        assertThat(answer.outputTokens()).isEqualTo(42);
        assertThat(answer.stopReason()).isEqualTo("end_turn");
    }

    @Test
    void sendsTheModelPromptEffortAndFallbackSettings() throws Exception {
        ClaudeAssistant claude = assistantAgainst(200, "text/event-stream", streamOf("end_turn", "ok"), 0);

        claude.ask(PROMPT, text -> { });

        assertThat(requestBody.get()).contains(
                "\"model\":\"claude-opus-5-5\"",
                "\"stream\":true",
                "\"system\":\"Explain DNS.\"",
                "Explain this dig result.",
                "\"effort\":\"medium\"",
                "\"fallbacks\":\"default\"");
        assertThat(betaHeader.get()).contains("server-side-fallback-2026-07-01");
    }

    @Test
    void aRefusalIsReportedAsSuch() throws Exception {
        ClaudeAssistant claude = assistantAgainst(200, "text/event-stream", streamOf("refusal"), 0);

        assertThatThrownBy(() -> claude.ask(PROMPT, text -> { }))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.kind()).isEqualTo(AiException.Kind.REFUSED));
    }

    @Test
    void aRejectedKeyBecomesAFriendlyUnauthorizedError() throws Exception {
        ClaudeAssistant claude = assistantAgainst(401, "application/json",
                "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}", 0);

        assertThatThrownBy(() -> claude.ask(PROMPT, text -> { }))
                .isInstanceOfSatisfying(AiException.class, e -> {
                    assertThat(e.kind()).isEqualTo(AiException.Kind.UNAUTHORIZED);
                    assertThat(e.getMessage()).contains("ANTHROPIC_API_KEY");
                });
    }

    @Test
    void rateLimitingIsReportedAsSuch() throws Exception {
        ClaudeAssistant claude = assistantAgainst(429, "application/json",
                "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"slow down\"}}", 0);

        assertThatThrownBy(() -> claude.ask(PROMPT, text -> { }))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.kind()).isEqualTo(AiException.Kind.RATE_LIMITED));
    }

    @Test
    void interruptingStopsTheRequestPromptlyEvenWhenNoTextArrives() throws Exception {
        // One event every 2 seconds, as during a long silent think.
        ClaudeAssistant claude = assistantAgainst(200, "text/event-stream",
                streamOf("end_turn", "a", "b", "c", "d", "e"), 2_000);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                claude.ask(PROMPT, text -> { });
            } catch (Throwable t) {
                outcome.set(t);
            }
        });
        caller.start();
        Thread.sleep(600);

        long start = System.nanoTime();
        caller.interrupt();
        caller.join(5_000);

        assertThat(caller.isAlive()).isFalse();
        assertThat((System.nanoTime() - start) / 1_000_000).as("ms to stop").isLessThan(1_500);
        assertThat(outcome.get()).isInstanceOfSatisfying(AiException.class,
                e -> assertThat(e.kind()).isEqualTo(AiException.Kind.CANCELLED));
    }

    @Test
    void isFoundOnTheClasspath() {
        assertThat(AiAssistant.discover()).get().isInstanceOf(ClaudeAssistant.class);
        assertThat(AiAssistant.discover().get().model()).isEqualTo(ClaudeAssistant.DEFAULT_MODEL);
    }

    @Test
    void withoutCredentialsItExplainsHowToSetUpAndSendsNothing() throws Exception {
        if (ClaudeAssistant.hasCredentials()) {
            return;   // this machine has a key; nothing to check
        }
        ClaudeAssistant claude = new ClaudeAssistant();

        assertThat(claude.setupHint()).get().asString().contains("ANTHROPIC_API_KEY");
        assertThatThrownBy(() -> claude.ask(PROMPT, text -> { }))
                .isInstanceOfSatisfying(AiException.class, e -> assertThat(e.kind()).isEqualTo(AiException.Kind.NOT_CONFIGURED));
    }
}
