package com.educa.backend.payment;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderAndProviderReference(PaymentProvider provider, String providerReference);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
