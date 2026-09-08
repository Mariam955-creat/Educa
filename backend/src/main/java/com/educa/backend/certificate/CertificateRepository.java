package com.educa.backend.certificate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Certificate> findByVerificationCode(String verificationCode);

    List<Certificate> findByUserIdOrderByIssuedAtDesc(Long userId);
}
