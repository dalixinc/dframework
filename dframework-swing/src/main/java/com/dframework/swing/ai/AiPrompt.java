package com.dframework.swing.ai;

import java.util.Objects;

/**
 * What is sent to the model.
 *
 * @param system instructions: the model's role and how to answer
 * @param user   the request, including any data to explain
 */
public record AiPrompt(String system, String user) {

    public AiPrompt {
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(user, "user");
    }

    /** The whole prompt as readable text, for showing the user exactly what was sent. */
    public String describe() {
        return "System instructions:\n" + system + "\n\nRequest:\n" + user;
    }
}
