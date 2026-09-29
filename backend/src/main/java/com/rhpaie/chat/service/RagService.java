package com.rhpaie.chat.service;

import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.faq.entity.ChatFaqDocument;
import com.rhpaie.chat.faq.repository.ChatFaqDocumentRepository;
import com.rhpaie.chat.faq.service.ChatFaqDocumentService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int PARAGRAPH_LIMIT = 800;
    private static final double VECTOR_MIN_SCORE = 0.32;
    /** Chosen on the FAQ dev split: weak raw scores need a clear top1-top2 gap. */
    private static final double WEAK_RAW_CEILING = 0.34;
    private static final double WEAK_MIN_MARGIN = 0.08;
    private static final Set<String> STOPWORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "je", "tu", "il", "elle",
            "nous", "vous", "mon", "ma", "mes", "comment", "quoi", "est", "ce", "que", "pour", "dans",
            "sur", "avec", "a", "au", "aux"
    );

    private final ChatFaqDocumentRepository chatFaqDocumentRepository;
    private final EmbeddingClient embeddingClient;
    private final List<FaqChunk> chunks = new CopyOnWriteArrayList<>();

    public RagService(ChatFaqDocumentRepository chatFaqDocumentRepository, EmbeddingClient embeddingClient) {
        this.chatFaqDocumentRepository = chatFaqDocumentRepository;
        this.embeddingClient = embeddingClient;
    }

    @PostConstruct
    public synchronized void reload() {
        List<FaqChunk> next = new ArrayList<>();
        loadBundled(next);
        loadUploaded(next);
        attachVectors(next);
        chunks.clear();
        chunks.addAll(next);
        long withVectors = next.stream().filter(chunk -> chunk.vector() != null).count();
        log.info("RAG loaded {} chunks ({} with CamemBERT embeddings)", next.size(), withVectors);
    }

    public String retrieve(String question, ChatIntent intent) {
        if (chunks.isEmpty()) {
            return "";
        }
        double[] queryVector = embeddingClient.embedOne(question);
        List<String> queryTokens = tokens(question);
        if (intent.isFaq()) {
            queryTokens.addAll(tokens(intent.name().replace('_', ' ')));
        }
        boolean useVectors = queryVector != null && chunks.stream().anyMatch(chunk -> chunk.vector() != null);
        if (!useVectors && queryTokens.isEmpty()) {
            return "";
        }
        List<ScoredChunk> ranked = chunks.stream()
                .map(chunk -> score(chunk, queryVector, queryTokens, intent, useVectors))
                .sorted(Comparator.comparingDouble(ScoredChunk::rankScore).reversed())
                .limit(3)
                .toList();
        List<ScoredChunk> visible = ranked.stream()
                .filter(item -> item.evidence() > (useVectors ? VECTOR_MIN_SCORE : 0))
                .toList();
        if (useVectors && !passesWeakMargin(ranked, visible)) {
            return "";
        }
        return visible.stream()
                .map(item -> item.chunk().title() + "\n" + item.chunk().body())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * A raw score above 0.34 is clear enough. Inside (0.32, 0.34], keep the hit only when
     * the ranked top1 itself clears 0.32 and leads top2 by at least 0.08.
     */
    private boolean passesWeakMargin(List<ScoredChunk> ranked, List<ScoredChunk> visible) {
        if (visible.isEmpty()) {
            return false;
        }
        double lead = visible.stream().mapToDouble(ScoredChunk::evidence).max().orElse(0);
        if (lead > WEAK_RAW_CEILING) {
            return true;
        }
        double raw1 = ranked.getFirst().evidence();
        double raw2 = ranked.size() > 1 ? ranked.get(1).evidence() : 0;
        return raw1 > VECTOR_MIN_SCORE && (raw1 - raw2) >= WEAK_MIN_MARGIN;
    }

    private void attachVectors(List<FaqChunk> target) {
        if (target.isEmpty()) {
            return;
        }
        List<String> texts = target.stream()
                .map(chunk -> chunk.title() + "\n" + chunk.body())
                .toList();
        List<double[]> vectors = embeddingClient.embed(texts);
        if (vectors.size() != target.size()) {
            return;
        }
        for (int i = 0; i < target.size(); i++) {
            FaqChunk chunk = target.get(i);
            target.set(i, new FaqChunk(chunk.title(), chunk.body(), chunk.intent(), chunk.tokens(), vectors.get(i)));
        }
    }

    private void loadBundled(List<FaqChunk> target) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:chat/faq/*.md");
            for (Resource resource : resources) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                String fileHint = resource.getFilename() == null ? "" : resource.getFilename();
                addChunks(target, content, fileIntent(fileHint));
            }
        } catch (IOException exception) {
            log.warn("Unable to load bundled FAQ documents: {}", exception.getMessage());
        }
    }

    private void loadUploaded(List<FaqChunk> target) {
        try {
            for (ChatFaqDocument document : chatFaqDocumentRepository.findAll()) {
                addChunks(target, document.getExtractedText(), ChatFaqDocumentService.intentFor(document.getCategory()));
            }
        } catch (Exception exception) {
            log.warn("Unable to load uploaded FAQ documents: {}", exception.getMessage());
        }
    }

    private void addChunks(List<FaqChunk> target, String content, ChatIntent intent) {
        if (content == null || content.isBlank()) {
            return;
        }
        if (content.contains("## ")) {
            String[] parts = content.split("(?m)^## ");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int lineBreak = trimmed.indexOf('\n');
                String title = lineBreak < 0 ? trimmed : trimmed.substring(0, lineBreak).trim();
                String body = lineBreak < 0 ? "" : trimmed.substring(lineBreak + 1).trim();
                if (title.startsWith("# ")) {
                    continue;
                }
                if (body.isBlank()) {
                    body = title;
                }
                target.add(new FaqChunk(title, body, intent, tokens(title + " " + body), null));
            }
            return;
        }
        String remaining = content.trim();
        int index = 1;
        while (!remaining.isBlank()) {
            int take = Math.min(PARAGRAPH_LIMIT, remaining.length());
            int split = remaining.lastIndexOf('\n', take);
            if (split < 120) {
                split = take;
            }
            String piece = remaining.substring(0, split).trim();
            if (!piece.isBlank()) {
                String title = piece.lines().findFirst().orElse("Section " + index);
                target.add(new FaqChunk(title, piece, intent, tokens(piece), null));
                index++;
            }
            remaining = remaining.substring(Math.min(split, remaining.length())).trim();
        }
    }

    private ScoredChunk score(
            FaqChunk chunk,
            double[] queryVector,
            List<String> queryTokens,
            ChatIntent intent,
            boolean useVectors
    ) {
        double evidence;
        if (useVectors && chunk.vector() != null && queryVector != null) {
            evidence = cosine(chunk.vector(), queryVector);
        } else {
            evidence = queryTokens.stream().filter(chunk.tokens()::contains).count();
        }
        double rankScore = evidence;
        if (chunk.intent() == intent) {
            rankScore += useVectors ? 0.06 : 2;
        } else if (chunk.intent() == ChatIntent.UNKNOWN && intent.isFaq()) {
            rankScore += useVectors ? 0.03 : 1;
        }
        return new ScoredChunk(chunk, rankScore, evidence);
    }

    private double cosine(double[] left, double[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private ChatIntent fileIntent(String filename) {
        if (filename.contains("conge")) {
            return ChatIntent.FAQ_LEAVE;
        }
        if (filename.contains("paie")) {
            return ChatIntent.FAQ_PAYROLL;
        }
        if (filename.contains("pointage")) {
            return ChatIntent.FAQ_ATTENDANCE;
        }
        return ChatIntent.UNKNOWN;
    }

    private List<String> tokens(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private record FaqChunk(String title, String body, ChatIntent intent, List<String> tokens, double[] vector) {
    }

    private record ScoredChunk(FaqChunk chunk, double rankScore, double evidence) {
    }
}
