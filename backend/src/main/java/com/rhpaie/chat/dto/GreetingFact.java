package com.rhpaie.chat.dto;

/**
 * Structured greeting fact (no free-form sensitive payloads).
 */
public record GreetingFact(
        String type,
        String value
) {
}
