package com.rhpaie.chat.service;

import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.entity.ChatMessage;
import com.rhpaie.user.entity.Role;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatMemorySupport {

    private static final Pattern YEAR = Pattern.compile("(20\\d{2})");

    public List<ChatMessage> oldestFirst(List<ChatMessage> newestFirst) {
        List<ChatMessage> copy = new ArrayList<>(newestFirst);
        Collections.reverse(copy);
        return copy;
    }

    public boolean looksLikeFollowUp(String message) {
        String raw = message == null ? "" : message.trim();
        if (raw.isBlank()) {
            return false;
        }
        String text = normalize(raw);
        if (raw.length() <= 24) {
            return true;
        }
        return containsAny(text,
                "aussi", "what about", "annee derniere", "l annee derniere",
                "last year", "celui", "celle", "la meme", "le meme", "idem")
                || text.matches(".*\\bet\\s+20\\d{2}\\b.*")
                || text.matches(".*\\bannee\\s+derniere.*");
    }

    public Integer resolveYear(String message) {
        String text = normalize(message);
        Matcher matcher = YEAR.matcher(text);
        Integer explicit = null;
        while (matcher.find()) {
            explicit = Integer.parseInt(matcher.group(1));
        }
        if (explicit != null && explicit >= 2020 && explicit <= 2100) {
            return explicit;
        }
        if (containsAny(text, "annee derniere", "l annee derniere", "last year", "an dernier")) {
            return LocalDate.now().getYear() - 1;
        }
        return null;
    }

    public ChatIntent reusableIntent(String lastIntentLabel, Role role) {
        ChatIntent intent = ChatIntent.fromLabel(lastIntentLabel);
        if ((intent.isTool() || intent.isFaq() || intent.isAction()) && intent.allowedFor(role)) {
            return intent;
        }
        return null;
    }

    public String formatHistory(List<ChatMessage> oldestFirst) {
        if (oldestFirst == null || oldestFirst.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ChatMessage turn : oldestFirst) {
            builder.append("Utilisateur: ").append(truncate(turn.getMessage(), 220)).append('\n');
            builder.append("Assistant: ").append(truncate(turn.getReply(), 220)).append('\n');
        }
        return builder.toString().trim();
    }

    public String lastUserMessage(List<ChatMessage> newestFirst) {
        if (newestFirst == null || newestFirst.isEmpty()) {
            return "";
        }
        return newestFirst.getFirst().getMessage();
    }

    public String lastIntentLabel(List<ChatMessage> newestFirst) {
        if (newestFirst == null || newestFirst.isEmpty()) {
            return null;
        }
        return newestFirst.getFirst().getIntent();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replace('\n', ' ').trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "…";
    }

    private String normalize(String message) {
        String normalized = Normalizer.normalize(message == null ? "" : message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return " " + normalized.replace("'", " ").replace("’", " ") + " ";
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
