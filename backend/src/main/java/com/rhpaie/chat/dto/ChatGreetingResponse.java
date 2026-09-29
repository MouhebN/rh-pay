package com.rhpaie.chat.dto;

import java.util.List;

/**
 * Proactive READ-only greeting for chat widget open.
 */
public record ChatGreetingResponse(
        String message,
        List<GreetingFact> facts,
        boolean draftOpen
) {
}
