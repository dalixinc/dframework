package com.dframework.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.NotFoundException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.dframework.swing.ai.AiAnswer;
import com.dframework.swing.ai.AiAssistant;
import com.dframework.swing.ai.AiException;
import com.dframework.swing.ai.AiPrompt;

/**
 * An {@link AiAssistant} backed by Claude, through the Anthropic Java SDK.
 *
 * <ul>
 * <li><b>Credentials:</b> the SDK's usual ones, normally the
 * {@code ANTHROPIC_API_KEY} environment variable, or a profile from
 * {@code ant auth login}.
 * <li><b>Model:</b> Claude Opus 5.5 ({@code claude-opus-5-5}). Override it
 * with the system property {@code dframework.ai.model}.
 * <li><b>Effort:</b> medium, which suits short explanations.
 * <li><b>Refusals:</b> uses the API's server-side fallback, so a request
 * declined by a safety classifier is retried on a suitable model instead of
 * failing.
 * </ul>
 */
public final class ClaudeAssistant implements AiAssistant {

    /** The model used unless {@code dframework.ai.model} says otherwise. */
    public static final String DEFAULT_MODEL = "claude-opus-5-5";

    private static final long MAX_TOKENS = 64_000L;
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    /** Watches for Stop; one shared daemon thread is plenty. */
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dframework-ai-cancel-watchdog");
        t.setDaemon(true);
        return t;
    });

    private final String model;
    private final boolean clientSupplied;
    private volatile AnthropicClient client;

    /** Uses the SDK's usual credentials; found by {@link AiAssistant#discover()}. */
    public ClaudeAssistant() {
        this.model = System.getProperty("dframework.ai.model", DEFAULT_MODEL);
        this.clientSupplied = false;
    }

    /** Uses the given client, e.g. one pointed at a test server. */
    ClaudeAssistant(AnthropicClient client, String model) {
        this.model = model;
        this.client = client;
        this.clientSupplied = true;
    }

    @Override
    public String name() {
        return "Claude";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String provider() {
        return "Anthropic";
    }

    @Override
    public Optional<String> setupHint() {
        return clientSupplied || hasCredentials() ? Optional.empty() : Optional.of(
                "To use Claude, set the ANTHROPIC_API_KEY environment variable (or run \"ant auth login\")"
                + " and restart the application. Get a key at https://platform.claude.com (Settings, API keys).");
    }

    @Override
    public AiAnswer ask(AiPrompt prompt, Consumer<String> onText) throws AiException {
        Optional<String> hint = setupHint();
        if (hint.isPresent()) {
            throw new AiException(AiException.Kind.NOT_CONFIGURED, hint.get(), null);
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(prompt.system())
                .addUserMessage(prompt.user())
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.MEDIUM).build())
                // If a safety classifier declines, let the API retry on a suitable model.
                .addBeta(FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();

        long start = System.nanoTime();
        StringBuilder text = new StringBuilder();
        Usage usage = new Usage(model);
        Thread caller = Thread.currentThread();
        try {
            StreamResponse<BetaRawMessageStreamEvent> stream = client().beta().messages().createStreaming(params);
            // While Claude thinks there may be no events for a while, so an interrupt check
            // between events isn't enough: the watchdog closes the connection (from its own
            // thread, which is why this isn't try-with-resources) as soon as Stop is pressed.
            ScheduledFuture<?> watchdog = WATCHDOG.scheduleAtFixedRate(() -> {
                if (caller.isInterrupted()) {
                    stream.close();
                }
            }, 200, 200, TimeUnit.MILLISECONDS);
            try {
                read(stream, onText, text, usage);
            } finally {
                watchdog.cancel(false);
                stream.close();
            }
            if (caller.isInterrupted()) {
                throw new AiException(AiException.Kind.CANCELLED, "Stopped.", null);
            }
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw new AiException(AiException.Kind.UNAUTHORIZED,
                    "Claude rejected the API key. Check ANTHROPIC_API_KEY and try again.", e);
        } catch (NotFoundException e) {
            throw new AiException(AiException.Kind.OTHER,
                    "The model \"" + model + "\" wasn't found. Check the dframework.ai.model setting.", e);
        } catch (RateLimitException e) {
            throw new AiException(AiException.Kind.RATE_LIMITED,
                    "Claude is busy (rate limited). Wait a moment and try again.", e);
        } catch (AnthropicServiceException e) {
            throw new AiException(AiException.Kind.OTHER, "Claude returned an error: " + e.getMessage(), e);
        } catch (AnthropicIoException e) {
            if (caller.isInterrupted()) {
                throw new AiException(AiException.Kind.CANCELLED, "Stopped.", e);
            }
            throw new AiException(AiException.Kind.NETWORK,
                    "Couldn't reach Claude. Check the internet connection and try again.", e);
        } catch (RuntimeException e) {
            // Closing the stream mid-read surfaces as some runtime exception from the HTTP layer.
            if (caller.isInterrupted()) {
                throw new AiException(AiException.Kind.CANCELLED, "Stopped.", e);
            }
            throw new AiException(AiException.Kind.OTHER, "Something went wrong talking to Claude: " + e, e);
        }
        if ("refusal".equals(usage.stopReason)) {
            throw new AiException(AiException.Kind.REFUSED, "Claude declined to explain this result.", null);
        }
        return new AiAnswer(text.toString(), usage.model, usage.inputTokens, usage.outputTokens, usage.stopReason,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    /** Running totals from the stream's events. */
    private static final class Usage {
        String model;
        long inputTokens;
        long outputTokens;
        String stopReason;

        Usage(String model) {
            this.model = model;
        }
    }

    private static void read(StreamResponse<BetaRawMessageStreamEvent> stream, Consumer<String> onText,
            StringBuilder text, Usage usage) {
        var events = stream.stream().iterator();
        while (events.hasNext()) {
            BetaRawMessageStreamEvent event = events.next();
            if (event.messageStart().isPresent()) {
                var message = event.messageStart().get().message();
                usage.model = message.model().asString();
                usage.inputTokens = message.usage().inputTokens();
            }
            event.contentBlockDelta()
                    .flatMap(delta -> delta.delta().text())
                    .ifPresent(delta -> {
                        text.append(delta.text());
                        onText.accept(delta.text());
                    });
            if (event.messageDelta().isPresent()) {
                var delta = event.messageDelta().get();
                usage.outputTokens = delta.usage().outputTokens();
                usage.stopReason = delta.delta().stopReason().map(BetaStopReason::asString).orElse(usage.stopReason);
            }
        }
    }

    private AnthropicClient client() throws AiException {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        client = AnthropicOkHttpClient.fromEnv();
                    } catch (RuntimeException e) {
                        throw new AiException(AiException.Kind.NOT_CONFIGURED,
                                "Claude isn't set up: " + e.getMessage(), e);
                    }
                }
                c = client;
            }
        }
        return c;
    }

    /**
     * Whether any credential source the SDK reads is present: an API key or
     * token, a named profile, workload identity federation, or a stored
     * {@code ant auth login} profile.
     */
    static boolean hasCredentials() {
        for (String variable : new String[] {"ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_PROFILE",
                "ANTHROPIC_FEDERATION_RULE_ID"}) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return Files.isDirectory(Path.of(System.getProperty("user.home"), ".config", "anthropic"));
    }
}
