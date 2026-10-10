package com.dframework.swing.ai;

import java.io.Serial;

/** A request to an {@link AiAssistant} that didn't produce an answer. The message is written for the user. */
public class AiException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /** What went wrong, so callers can react (for example, offer setup help for {@link #NOT_CONFIGURED}). */
    public enum Kind {
        /** No credentials, so the assistant can't be used yet. */
        NOT_CONFIGURED,
        /** Credentials were rejected. */
        UNAUTHORIZED,
        /** Too many requests; try again shortly. */
        RATE_LIMITED,
        /** The service couldn't be reached. */
        NETWORK,
        /** The model declined to answer. */
        REFUSED,
        /** The user stopped the request. */
        CANCELLED,
        /** Anything else. */
        OTHER
    }

    private final Kind kind;

    public AiException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
