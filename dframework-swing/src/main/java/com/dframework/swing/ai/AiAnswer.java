package com.dframework.swing.ai;

/**
 * A finished answer.
 *
 * @param text         the full answer
 * @param model        the model that answered (may differ from the one asked if the provider fell back)
 * @param inputTokens  tokens sent
 * @param outputTokens tokens received
 * @param stopReason   why the answer ended, e.g. "end_turn"
 * @param durationMs   wall-clock time for the request
 */
public record AiAnswer(String text, String model, long inputTokens, long outputTokens, String stopReason, long durationMs) {
}
