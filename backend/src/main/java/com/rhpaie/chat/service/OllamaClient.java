package com.rhpaie.chat.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rhpaie.chat.config.ChatProperties;
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
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final ChatProperties properties;
    private final RestClient restClient;

    public OllamaClient(ChatProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(25));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getOllama().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public String generate(String question, String groundedContext, String history) {
        String historyBlock = history == null || history.isBlank()
                ? "(aucun)"
                : history;
        String prompt = """
                Tu es l'assistant RH de l'application RH Paie.
                Réponds en français, de façon courte (2 à 4 phrases), professionnelle et utile.
                Les données ci-dessous viennent de la base RH Paie : elles sont EXACTES.
                Tu DOIS t'en servir. Reprends les chiffres tels quels.
                Interdiction de dire que tu n'as pas l'information si les données sont présentes.
                N'invente aucun chiffre, nom ou statut.
                Ne propose pas d'approuver un congé ni de lancer un calcul de paie.
                L'historique sert uniquement à comprendre les pronoms et les suites de conversation.
                Les chiffres doivent venir UNIQUEMENT des données fiables, pas de l'historique.

                Historique récent :
                %s

                Données fiables :
                %s

                Question de l'utilisateur :
                %s
                """.formatted(
                historyBlock,
                groundedContext == null || groundedContext.isBlank() ? "(aucune)" : groundedContext,
                question);
        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getOllama().getModel(),
                            "prompt", prompt,
                            "stream", false
                    ))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
            if (response == null || response.response() == null || response.response().isBlank()) {
                return null;
            }
            return response.response().trim();
        } catch (RestClientException exception) {
            log.warn("Ollama unavailable, using template reply: {}", exception.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaGenerateResponse(String response) {
    }
}
