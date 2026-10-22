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

    List<Payment> findByKindAndStatus(PaymentKind kind, PaymentStatus status);

    /** Ligne {@code [abonnés dont l'accès court après now, montant total des pass]}. */
    @org.springframework.data.jpa.repository.Query("""
            select count(distinct case when p.accessUntil > :now then p.userId end), coalesce(sum(p.amount), 0)
            from Payment p where p.kind = com.educa.backend.payment.PaymentKind.SUBSCRIPTION
              and p.status = com.educa.backend.payment.PaymentStatus.SUCCEEDED""")
    List<Object[]> subscriptionTotals(@org.springframework.data.repository.query.Param("now") java.time.Instant now);

    /** Pass d'abonnement réussi dont l'accès court encore après {@code now}. */
    boolean existsByUserIdAndKindAndStatusAndAccessUntilAfter(Long userId, PaymentKind kind, PaymentStatus status,
                                                               java.time.Instant now);

    /** Dernier pass d'abonnement réussi (accès le plus lointain), pour prolonger ou afficher l'abonnement. */
    Optional<Payment> findFirstByUserIdAndKindAndStatusOrderByAccessUntilDesc(Long userId, PaymentKind kind,
                                                                             PaymentStatus status);

    /** Ligne {@code [somme, nombre]} des paiements d'un statut depuis une date. */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(p.amount), 0), count(p) from Payment p
            where p.status = :status and p.createdAt >= :since
            """)
    List<Object[]> totalsSince(@org.springframework.data.repository.query.Param("status") PaymentStatus status,
                               @org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
