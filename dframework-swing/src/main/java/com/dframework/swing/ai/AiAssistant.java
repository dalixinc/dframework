package com.dframework.swing.ai;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * An AI model that can answer a question about something on screen, such as
 * "explain this DNS result". Kept independent of any provider: the
 * library defines the interface, and an implementation such as the
 * {@code dframework-ai} module's Claude assistant is found at runtime.
 *
 * <p>Implementations are registered in
 * {@code META-INF/services/com.dframework.swing.ai.AiAssistant} and found with
 * {@link #discover()}.
 */
public interface AiAssistant {

    /** Display name, e.g. "Claude". */
    String name();

    /** The model used, e.g. "claude-opus-5-5". */
    String model();

    /** Who receives the prompt, for telling the user where their data goes, e.g. "Anthropic". */
    String provider();

    /**
     * If the assistant can't be used yet (no credentials, say), what the user
     * needs to do; empty when it is ready.
     */
    default Optional<String> setupHint() {
        return Optional.empty();
    }

    /**
     * Sends {@code prompt} and streams the answer to {@code onText} as it is
     * written. Blocks until finished, so call it off the event dispatch thread.
     * Interrupting the calling thread stops the request.
     *
     * @throws AiException if the request can't be made or is refused; its
     *                     message is suitable to show the user
     */
    AiAnswer ask(AiPrompt prompt, Consumer<String> onText) throws AiException;

    /** The first assistant found on the classpath, if any. */
    static Optional<AiAssistant> discover() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            return ServiceLoader.load(AiAssistant.class, loader != null ? loader : AiAssistant.class.getClassLoader())
                    .findFirst();
        } catch (ServiceConfigurationError e) {
            System.getLogger(AiAssistant.class.getName()).log(System.Logger.Level.ERROR, "Cannot load AI assistant", e);
            return Optional.empty();
        }
    }
}
