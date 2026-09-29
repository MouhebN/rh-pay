package com.rhpaie.chat.repository;

import com.rhpaie.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop3ByUser_IdOrderByCreatedAtDesc(Long userId);
}
