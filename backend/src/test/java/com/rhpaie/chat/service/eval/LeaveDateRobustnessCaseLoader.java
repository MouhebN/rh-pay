package com.rhpaie.chat.service.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads robustness JSON resources (MASSIVE-adapted HR leave utterances).
 */
final class LeaveDateRobustnessCaseLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LeaveDateRobustnessCaseLoader() {
    }

    static List<DateEvalCase> load(String classpathResource) {
        try (InputStream in = LeaveDateRobustnessCaseLoader.class
                .getClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + classpathResource);
            }
            JsonNode root = MAPPER.readTree(in);
            JsonNode cases = root.get("cases");
            if (cases == null || !cases.isArray()) {
                throw new IllegalStateException("Resource has no cases array: " + classpathResource);
            }
            List<DateEvalCase> out = new ArrayList<>(cases.size());
            for (JsonNode node : cases) {
                String name = text(node, "name");
                String category = text(node, "category");
                String input = text(node, "input");
                String origin = node.hasNonNull("origin") ? node.get("origin").asText() : null;
                boolean clarify = node.path("shouldAskClarification").asBoolean(false);
                if (clarify) {
                    out.add(DateEvalCase.clarify(name, category, input, origin));
                } else {
                    LocalDate start = LocalDate.parse(text(node, "expectedStart"));
                    LocalDate end = LocalDate.parse(text(node, "expectedEnd"));
                    out.add(DateEvalCase.dates(name, category, input, start, end, origin));
                }
            }
            return out;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load " + classpathResource, exception);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Missing field '" + field + "' in case: " + node);
        }
        return value.asText();
    }
}
