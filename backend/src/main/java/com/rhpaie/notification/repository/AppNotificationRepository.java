package com.rhpaie.notification.repository;

import com.rhpaie.notification.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findTop10ByRecipientUsernameOrderByCreatedAtDesc(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppNotification n set n.read = true where n.recipient.username = :username and n.read = false")
    int markAllReadByRecipientUsername(@Param("username") String username);
}
