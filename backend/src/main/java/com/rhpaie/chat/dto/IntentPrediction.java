package com.rhpaie.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentPrediction(
        String intent,
        Double confidence
) {
}
