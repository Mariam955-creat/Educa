package com.educa.backend.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRequestDocumentRepository extends JpaRepository<InstructorRequestDocument, Long> {

    List<InstructorRequestDocument> findByRequestIdInOrderByIdAsc(Collection<Long> requestIds);

    Optional<InstructorRequestDocument> findByIdAndRequestId(Long id, Long requestId);
}
