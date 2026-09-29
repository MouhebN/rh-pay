package com.rhpaie.notification.service;

import com.rhpaie.notification.dto.NotificationResponse;
import com.rhpaie.notification.entity.AppNotification;
import com.rhpaie.notification.entity.NotificationType;
import com.rhpaie.notification.mapper.NotificationMapper;
import com.rhpaie.notification.repository.AppNotificationRepository;
import com.rhpaie.notification.websocket.NotificationWebSocketHandler;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppNotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketHandler webSocketHandler;

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent(String username) {
        return notificationRepository.findTop10ByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllReadByRecipientUsername(username);
    }

    @Transactional
    public void notifyAdmins(String title, String message, NotificationType type, String link) {
        userRepository.findByRole(Role.ADMIN_RH)
                .forEach(admin -> notifyUser(admin, title, message, type, link));
    }

    @Transactional
    public void notifyUsername(String username, String title, String message, NotificationType type, String link) {
        userRepository.findByUsername(username)
                .ifPresent(user -> notifyUser(user, title, message, type, link));
    }

    private void notifyUser(UserAccount recipient, String title, String message, NotificationType type, String link) {
        AppNotification notification = notificationRepository.save(AppNotification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .build());
        NotificationResponse response = notificationMapper.toResponse(notification);
        webSocketHandler.sendToUser(recipient.getUsername(), response);
    }
}
