package com.educa.backend.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRequestRepository extends JpaRepository<InstructorRequest, Long> {

    Page<InstructorRequest> findByStatus(InstructorRequest.Status status, Pageable pageable);

    long countByStatus(InstructorRequest.Status status);

    boolean existsByUserIdAndStatus(Long userId, InstructorRequest.Status status);

    Optional<InstructorRequest> findByUserIdAndStatus(Long userId, InstructorRequest.Status status);

    /** Dernière demande du compte (en attente ou traitée). */
    Optional<InstructorRequest> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
