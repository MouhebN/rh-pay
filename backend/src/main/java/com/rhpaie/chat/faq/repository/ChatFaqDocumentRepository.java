package com.rhpaie.chat.faq.repository;

import com.rhpaie.chat.faq.entity.ChatFaqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatFaqDocumentRepository extends JpaRepository<ChatFaqDocument, Long> {

    @Query("select d from ChatFaqDocument d left join fetch d.uploadedBy order by d.createdAt desc")
    List<ChatFaqDocument> findAllByOrderByCreatedAtDesc();
}
