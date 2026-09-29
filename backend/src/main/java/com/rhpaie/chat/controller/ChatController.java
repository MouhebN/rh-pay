package com.rhpaie.chat.controller;

import com.rhpaie.chat.dto.ChatCancelLeaveActionRequest;
import com.rhpaie.chat.dto.ChatGreetingResponse;
import com.rhpaie.chat.dto.ChatLeaveActionRequest;
import com.rhpaie.chat.dto.ChatLeaveReviewActionRequest;
import com.rhpaie.chat.dto.ChatMessageRequest;
import com.rhpaie.chat.dto.ChatMessageResponse;
import com.rhpaie.chat.service.ChatGreetingService;
import com.rhpaie.chat.service.ChatService;
import com.rhpaie.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatGreetingService chatGreetingService;

    @GetMapping("/greeting")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatGreetingResponse> greeting(Principal principal) {
        ChatGreetingResponse greeting = chatGreetingService.greetingFor(principal.getName());
        return ApiResponse.success(
                "Chat greeting generated",
                new ChatGreetingResponse(
                        greeting.message(),
                        greeting.facts(),
                        chatService.hasOpenDraft(principal.getName())
                )
        );
    }

    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatMessageResponse> send(@Valid @RequestBody ChatMessageRequest request, Principal principal) {
        return ApiResponse.success("Chat reply generated", chatService.reply(principal.getName(), request.message().trim()));
    }

    @PostMapping("/actions/leave-request")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<ChatMessageResponse> confirmLeave(
            @Valid @RequestBody ChatLeaveActionRequest request,
            Principal principal
    ) {
        return ApiResponse.success(
                "Leave request created from chat",
                chatService.confirmLeaveRequest(principal.getName(), request)
        );
    }

    @PostMapping("/actions/leave-request/cancel")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<ChatMessageResponse> confirmCancelLeave(
            @Valid @RequestBody ChatCancelLeaveActionRequest request,
            Principal principal
    ) {
        return ApiResponse.success(
                "Leave request cancelled from chat",
                chatService.confirmCancelLeaveRequest(principal.getName(), request)
        );
    }

    @PostMapping("/actions/leave-review")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ChatMessageResponse> confirmLeaveReview(
            @Valid @RequestBody ChatLeaveReviewActionRequest request,
            Principal principal
    ) {
        return ApiResponse.success(
                "Leave review applied from chat",
                chatService.confirmLeaveReview(principal.getName(), request)
        );
    }

    @PostMapping("/actions/leave-request/discard")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'ADMIN_RH')")
    public ApiResponse<ChatMessageResponse> discardLeaveAction(Principal principal) {
        return ApiResponse.success(
                "Leave action discarded",
                chatService.discardLeaveAction(principal.getName())
        );
    }
}
