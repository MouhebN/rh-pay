package com.rhpaie.chat.faq.service;

import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.faq.dto.ChatFaqDocumentResponse;
import com.rhpaie.chat.faq.entity.ChatFaqDocument;
import com.rhpaie.chat.faq.entity.FaqCategory;
import com.rhpaie.chat.faq.repository.ChatFaqDocumentRepository;
import com.rhpaie.chat.service.RagService;
import com.rhpaie.common.config.FileStorageProperties;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatFaqDocumentService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("md", "txt", "pdf");

    private final ChatFaqDocumentRepository repository;
    private final UserRepository userRepository;
    private final FileStorageProperties fileStorageProperties;
    private final RagService ragService;

    @Transactional(readOnly = true)
    public List<ChatFaqDocumentResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChatFaqDocumentResponse upload(String title, String categoryValue, MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un fichier est requis.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier ne doit pas dépasser 5 Mo.");
        }
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String extension = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formats acceptés : md, txt, pdf.");
        }
        FaqCategory category = parseCategory(categoryValue);
        String extracted = extractText(file, extension);
        if (extracted.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'extraire du texte de ce fichier.");
        }
        Path stored = store(file, extension);
        UserAccount uploader = userRepository.findByUsername(username).orElse(null);
        String resolvedTitle = title == null || title.isBlank()
                ? originalName.replaceFirst("\\.[^.]+$", "")
                : title.trim();
        ChatFaqDocument saved = repository.save(ChatFaqDocument.builder()
                .title(resolvedTitle)
                .category(category)
                .fileName(originalName)
                .contentType(file.getContentType())
                .storagePath(stored.toString())
                .extractedText(extracted)
                .uploadedBy(uploader)
                .build());
        reloadAfterCommit();
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        ChatFaqDocument document = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document FAQ introuvable."));
        try {
            Files.deleteIfExists(Path.of(document.getStoragePath()));
        } catch (IOException ignored) {
            // Best-effort disk cleanup.
        }
        repository.delete(document);
        reloadAfterCommit();
    }

    private void reloadAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ragService.reload();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ragService.reload();
            }
        });
    }

    public static ChatIntent intentFor(FaqCategory category) {
        return switch (category) {
            case LEAVE -> ChatIntent.FAQ_LEAVE;
            case PAYROLL -> ChatIntent.FAQ_PAYROLL;
            case ATTENDANCE -> ChatIntent.FAQ_ATTENDANCE;
            case GENERAL -> ChatIntent.UNKNOWN;
        };
    }

    private String extractText(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            if ("pdf".equals(extension)) {
                try (PDDocument document = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(document).trim();
                }
            }
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible.");
        }
    }

    private Path store(MultipartFile file, String extension) {
        try {
            Path directory = Path.of(fileStorageProperties.getChatFaqUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String storedName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + UUID.randomUUID().toString().substring(0, 8)
                    + "." + extension;
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide.");
            }
            Files.write(target, file.getBytes());
            return target;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer le fichier.");
        }
    }

    private FaqCategory parseCategory(String value) {
        try {
            return FaqCategory.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie invalide.");
        }
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private ChatFaqDocumentResponse toResponse(ChatFaqDocument document) {
        return ChatFaqDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .category(document.getCategory())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .uploadedByUsername(document.getUploadedBy() == null ? null : document.getUploadedBy().getUsername())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
