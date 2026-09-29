package com.rhpaie.chat.repository;

import com.rhpaie.chat.entity.ChatActionDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatActionDraftRepository extends JpaRepository<ChatActionDraft, Long> {

    Optional<ChatActionDraft> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
