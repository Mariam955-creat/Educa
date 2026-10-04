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

    /** Ligne {@code [somme, nombre]} des paiements d'un statut depuis une date. */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(p.amount), 0), count(p) from Payment p
            where p.status = :status and p.createdAt >= :since
            """)
    List<Object[]> totalsSince(@org.springframework.data.repository.query.Param("status") PaymentStatus status,
                               @org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
