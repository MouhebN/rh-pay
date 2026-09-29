package com.rhpaie.chat.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Accent-folding + common typo fixes + light edit-distance keyword matching
 * for chatbot action parsers (not used for free-form LLM understanding).
 */
public final class TextNormalize {

    private static final Map<String, String> TOKEN_TYPOS = tokenTypos();
    private static final Set<String> KEYWORDS = Set.of(
            "accepter", "accepte", "approuver", "approuve", "approve", "valider", "valide",
            "refuser", "refuse", "rejeter", "rejette", "reject",
            "tous", "toutes", "tout", "all",
            "annuler", "annule", "demain", "weekend", "conge", "absence", "demande",
            "sauf", "premier", "premiers", "dernier", "derniers"
    );

    private TextNormalize() {
    }

    public static String fold(String message) {
        if (message == null) {
            return "";
        }
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replace("'", " ").replace("’", " ").replace('-', ' ');
    }

    /**
     * Fold accents then rewrite common misspellings and near-keyword tokens (distance ≤ 1).
     */
    public static String soften(String message) {
        String folded = fold(message);
        if (folded.isBlank()) {
            return folded;
        }
        String[] rawTokens = folded.split("\\s+");
        List<String> out = new ArrayList<>(rawTokens.length);
        for (String token : rawTokens) {
            if (token.isBlank()) {
                continue;
            }
            String cleaned = token.replaceAll("[^a-z0-9]", "");
            if (cleaned.isBlank()) {
                out.add(token);
                continue;
            }
            String mapped = TOKEN_TYPOS.getOrDefault(cleaned, cleaned);
            String fixed = closestKeyword(mapped);
            out.add(fixed);
        }
        return String.join(" ", out);
    }

    public static boolean containsAny(String text, String... needles) {
        String haystack = text == null ? "" : text;
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static int editDistance(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > 1) {
            return 2;
        }
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[m];
    }

    private static String closestKeyword(String token) {
        if (KEYWORDS.contains(token) || token.length() < 4 || token.length() > 14) {
            return token;
        }
        String best = token;
        int bestDist = 2;
        for (String keyword : KEYWORDS) {
            if (keyword.length() < 4 || Math.abs(keyword.length() - token.length()) > 1) {
                continue;
            }
            int dist = editDistance(token, keyword);
            if (dist <= 1 && dist < bestDist) {
                bestDist = dist;
                best = keyword;
            }
        }
        return best;
    }

    private static Map<String, String> tokenTypos() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("acccepter", "accepter");
        map.put("aceptar", "accepter");
        map.put("aceptter", "accepter");
        map.put("approuverr", "approuver");
        map.put("aproove", "approve");
        map.put("aprov", "approve");
        map.put("tus", "tous");
        map.put("tou", "tous");
        map.put("touts", "tous");
        map.put("toute", "toutes");
        map.put("abscence", "absence");
        map.put("absense", "absence");
        map.put("anuller", "annuler");
        map.put("anulle", "annule");
        map.put("anuler", "annuler");
        map.put("refuser", "refuser");
        map.put("réfuser", "refuser");
        map.put("reffuse", "refuse");
        map.put("demmain", "demain");
        map.put("deman", "demain");
        map.put("demian", "demain");
        map.put("deamin", "demain");
        map.put("apre", "apres");
        map.put("prochin", "prochain");
        map.put("septembr", "septembre");
        map.put("weekende", "weekend");
        map.put("weekend", "weekend");
        map.put("conge", "conge");
        map.put("conges", "conges");
        return map;
    }
}
