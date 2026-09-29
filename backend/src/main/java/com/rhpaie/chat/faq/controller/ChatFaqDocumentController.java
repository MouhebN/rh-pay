package com.rhpaie.chat.faq.controller;

import com.rhpaie.chat.faq.dto.ChatFaqDocumentResponse;
import com.rhpaie.chat.faq.service.ChatFaqDocumentService;
import com.rhpaie.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat/faq-documents")
@RequiredArgsConstructor
public class ChatFaqDocumentController {

    private final ChatFaqDocumentService chatFaqDocumentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ChatFaqDocumentResponse>> findAll() {
        return ApiResponse.success("FAQ documents loaded", chatFaqDocumentService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ChatFaqDocumentResponse> upload(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        return ApiResponse.success(
                "FAQ document uploaded",
                chatFaqDocumentService.upload(title, category, file, principal.getName())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        chatFaqDocumentService.delete(id);
        return ApiResponse.success("FAQ document deleted");
    }
}
