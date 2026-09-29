package com.rhpaie.chat.service;

import com.rhpaie.chat.config.ChatProperties;
import com.rhpaie.chat.dto.IntentPrediction;
import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.user.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class IntentClassifierClient {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifierClient.class);

    private final ChatProperties properties;
    private final KeywordIntentFallback keywordFallback;
    private final RestClient restClient;

    public IntentClassifierClient(ChatProperties properties, KeywordIntentFallback keywordFallback) {
        this.properties = properties;
        this.keywordFallback = keywordFallback;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getIntentUrl())
                .requestFactory(factory)
                .build();
    }

    public IntentPrediction predict(String message) {
        try {
            IntentPrediction prediction = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message))
                    .retrieve()
                    .body(IntentPrediction.class);
            if (prediction == null || prediction.intent() == null) {
                return keywordFallback.predict(message);
            }
            return choose(prediction, keywordFallback.predict(message), properties.getMinConfidence());
        } catch (RestClientException exception) {
            log.warn("Intent service unavailable, using keyword fallback: {}", exception.getMessage());
            return keywordFallback.predict(message);
        }
    }

    /**
     * A confident model label is kept. Below {@code minConfidence}, a confident keyword
     * match is used. If both are weak, the result is {@link ChatIntent#UNKNOWN} so the
     * assistant does not execute a business tool on an untrusted label.
     * Role checks stay in {@link #predictForRole}.
     */
    static IntentPrediction choose(IntentPrediction model, IntentPrediction keyword, double minConfidence) {
        ChatIntent intent = model == null ? ChatIntent.UNKNOWN : ChatIntent.fromLabel(model.intent());
        double confidence = model == null || model.confidence() == null ? 0.0 : model.confidence();
        if (intent != ChatIntent.UNKNOWN && confidence >= minConfidence) {
            return new IntentPrediction(intent.name(), confidence);
        }
        ChatIntent keywordIntent = keyword == null ? ChatIntent.UNKNOWN : ChatIntent.fromLabel(keyword.intent());
        double keywordConfidence = keyword == null || keyword.confidence() == null ? 0.0 : keyword.confidence();
        if (keywordIntent != ChatIntent.UNKNOWN && keywordConfidence >= minConfidence) {
            return keyword;
        }
        double reported = Math.max(confidence, keywordConfidence);
        return new IntentPrediction(ChatIntent.UNKNOWN.name(), reported);
    }

    public IntentPrediction predictForRole(String message, Role role) {
        IntentPrediction prediction = predict(message);
        ChatIntent intent = ChatIntent.fromLabel(prediction.intent());
        if (intent.isTool() && !intent.allowedFor(role)) {
            IntentPrediction fallback = keywordFallback.predict(message);
            ChatIntent fallbackIntent = ChatIntent.fromLabel(fallback.intent());
            if (fallbackIntent.allowedFor(role)
                    && (fallbackIntent.isTool() || fallbackIntent.isFaq() || fallbackIntent.isAction())) {
                return fallback;
            }
        }
        if (intent.isAction() && !intent.allowedFor(role)) {
            IntentPrediction fallback = keywordFallback.predict(message);
            ChatIntent fallbackIntent = ChatIntent.fromLabel(fallback.intent());
            if (fallbackIntent.allowedFor(role)
                    && (fallbackIntent.isTool() || fallbackIntent.isFaq() || fallbackIntent.isAction())) {
                return fallback;
            }
        }
        return prediction;
    }
}
