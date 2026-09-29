package com.rhpaie.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private String intentUrl = "http://localhost:8010";

    private double minConfidence = 0.55;

    private final Ollama ollama = new Ollama();

    @Getter
    @Setter
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2:3b";
    }
}
