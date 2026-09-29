package com.rhpaie.chat.service;

import com.rhpaie.chat.config.ChatProperties;
import com.rhpaie.chat.dto.EmbedRequest;
import com.rhpaie.chat.dto.EmbedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final int BATCH_SIZE = 32;

    private final RestClient restClient;

    public EmbeddingClient(ChatProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getIntentUrl())
                .requestFactory(factory)
                .build();
    }

    public double[] embedOne(String text) {
        List<double[]> vectors = embed(List.of(text == null ? "" : text));
        if (vectors.isEmpty()) {
            return null;
        }
        return vectors.getFirst();
    }

    public List<double[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            List<double[]> all = new ArrayList<>(texts.size());
            for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
                List<String> batch = texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()));
                EmbedResponse response = restClient.post()
                        .uri("/embed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new EmbedRequest(batch))
                        .retrieve()
                        .body(EmbedResponse.class);
                if (response == null || response.vectors() == null || response.vectors().size() != batch.size()) {
                    log.warn("Embedding service returned an unexpected payload");
                    return List.of();
                }
                for (List<Double> vector : response.vectors()) {
                    all.add(toArray(vector));
                }
            }
            return all;
        } catch (RestClientException exception) {
            log.warn("Embedding service unavailable, using keyword RAG: {}", exception.getMessage());
            return List.of();
        }
    }

    private double[] toArray(List<Double> vector) {
        double[] values = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            values[i] = vector.get(i) == null ? 0.0 : vector.get(i);
        }
        return values;
    }
}
