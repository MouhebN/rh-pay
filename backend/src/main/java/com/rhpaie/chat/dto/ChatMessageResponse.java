package com.rhpaie.chat.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatMessageResponse(
        String reply,
        String link,
        String linkLabel,
        String intent,
        String sourceKind,
        String sourceLabel,
        List<String> suggestions,
        ChatProposedAction proposedAction,
        boolean draftOpen
) {
}
