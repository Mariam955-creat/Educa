package com.educa.backend.ai;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Derniers messages d'une conversation, du plus récent au plus ancien (à inverser pour l'affichage). */
    List<ChatMessage> findByUserIdAndCourseIdOrderByIdDesc(Long userId, Long courseId, Pageable pageable);

    @Modifying
    @Query("delete from ChatMessage m where m.userId = :userId and m.courseId = :courseId")
    int deleteConversation(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
