package com.rhpaie.employeeexit.service;

import com.rhpaie.common.config.FileStorageProperties;
import com.rhpaie.employeeexit.dto.EmployeeExitDocumentResource;
import com.rhpaie.employeeexit.dto.EmployeeExitDocumentResponse;
import com.rhpaie.employeeexit.entity.EmployeeExit;
import com.rhpaie.employeeexit.entity.EmployeeExitDocument;
import com.rhpaie.employeeexit.entity.EmployeeExitDocumentType;
import com.rhpaie.employeeexit.repository.EmployeeExitDocumentRepository;
import com.rhpaie.employeeexit.repository.EmployeeExitRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeExitDocumentServiceImpl implements EmployeeExitDocumentService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");

    private final EmployeeExitDocumentRepository documentRepository;
    private final EmployeeExitRepository employeeExitRepository;
    private final UserRepository userRepository;
    private final FileStorageProperties fileStorageProperties;

    @Override
    @Transactional
    public EmployeeExitDocumentResponse upload(Long exitId, MultipartFile file, EmployeeExitDocumentType type, Boolean visibleToEmployee, String username) {
        validateDocument(file);
        EmployeeExit exit = findExit(exitId);
        UserAccount uploadedBy = findUser(username);
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        String storedFileName = exitId + "-" + UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path uploadDirectory = resolveUploadDirectory();
        Path targetPath = uploadDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store employee exit document");
        }

        EmployeeExitDocument document = EmployeeExitDocument.builder()
                .employeeExit(exit)
                .type(type)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .storagePath(targetPath.toString())
                .fileSize(file.getSize())
                .visibleToEmployee(Boolean.TRUE.equals(visibleToEmployee))
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .build();
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitDocumentResponse> findByExit(Long exitId) {
        return documentRepository.findByEmployeeExitIdOrderByUploadedAtDesc(exitId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeExitDocumentResource download(Long exitId, Long documentId) {
        return toResource(findDocument(exitId, documentId));
    }

    @Override
    @Transactional
    public void delete(Long exitId, Long documentId) {
        EmployeeExitDocument document = findDocument(exitId, documentId);
        deleteStoredFile(document);
        documentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeExitDocumentResponse> findVisibleMine(String username) {
        return documentRepository.findVisibleForEmployee(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeExitDocumentResource downloadVisibleMine(String username, Long documentId) {
        EmployeeExitDocument document = documentRepository.findVisibleForEmployeeById(username, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return toResource(document);
    }

    private EmployeeExitDocument findDocument(Long exitId, Long documentId) {
        return documentRepository.findByIdAndEmployeeExitId(documentId, exitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private EmployeeExit findExit(Long exitId) {
        return employeeExitRepository.findById(exitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee exit not found"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document size must not exceed 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, PNG, JPG and JPEG files are accepted");
        }
    }

    private Path resolveUploadDirectory() {
        return Paths.get(fileStorageProperties.getEmployeeExitUploadDir()).toAbsolutePath().normalize();
    }

    private EmployeeExitDocumentResource toResource(EmployeeExitDocument document) {
        try {
            Resource resource = new UrlResource(Paths.get(document.getStoragePath()).normalize().toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
            }
            return new EmployeeExitDocumentResource(resource, document.getOriginalFileName(), document.getContentType(), document.getFileSize());
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read document");
        }
    }

    private void deleteStoredFile(EmployeeExitDocument document) {
        try {
            Files.deleteIfExists(Paths.get(document.getStoragePath()).normalize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete document file");
        }
    }

    private EmployeeExitDocumentResponse toResponse(EmployeeExitDocument document) {
        return EmployeeExitDocumentResponse.builder()
                .id(document.getId())
                .employeeExitId(document.getEmployeeExit().getId())
                .type(document.getType())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .visibleToEmployee(document.getVisibleToEmployee())
                .uploadedByUsername(document.getUploadedBy() == null ? null : document.getUploadedBy().getUsername())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
