package com.educa.backend.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);
}
