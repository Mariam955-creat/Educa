package com.educa.backend.enrollment;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.educa.backend.payment.SubscriptionUsageProvider;

/** Inscriptions obtenues par abonnement, lues par {@code payment} pour la répartition des revenus. */
@Component
class EnrollmentSubscriptionUsageProvider implements SubscriptionUsageProvider {

    private final EnrollmentRepository enrollmentRepository;

    EnrollmentSubscriptionUsageProvider(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public List<SubscriptionUse> subscriptionUses(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.findByUserIdInAndViaSubscriptionTrue(userIds).stream()
                .map(e -> new SubscriptionUse(e.getUserId(), e.getCourseId(), e.getEnrolledAt()))
                .toList();
    }
}
