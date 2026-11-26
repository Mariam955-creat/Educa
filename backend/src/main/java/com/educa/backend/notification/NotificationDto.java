package com.educa.backend.notification;

import java.time.Instant;

public record NotificationDto(Long id, String type, String subject, String detail, String link, boolean read,
        Instant createdAt) {

    static NotificationDto of(Notification n) {
        return new NotificationDto(n.getId(), n.getType().name(), n.getSubject(), n.getDetail(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }
}
