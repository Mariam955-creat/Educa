package com.educa.backend.payment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderAndProviderReference(PaymentProvider provider, String providerReference);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, PaymentStatus status);

    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Payment> findByCourseIdInAndStatusOrderByCreatedAtDesc(Collection<Long> courseIds, PaymentStatus status);

    long countByInvoiceNumberIsNotNull();
}
