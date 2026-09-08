package com.educa.backend.enrollment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    boolean existsByEnrollmentIdAndContentId(Long enrollmentId, Long contentId);

    long countByEnrollmentId(Long enrollmentId);

    List<Progress> findByEnrollmentId(Long enrollmentId);
}
