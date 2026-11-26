package com.educa.backend.notification;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ResourceNotFoundException;

@Service
public class NotificationService {

    /** Nombre de notifications renvoyées dans la liste de la cloche. */
    static final int LIST_SIZE = 20;

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Envoie une notification à un utilisateur (enregistrée, affichée à sa prochaine visite). */
    @Transactional
    public void notify(Long userId, NotificationType type, String detail, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setDetail(detail);
        notification.setLink(link);
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationListDto list(Long userId) {
        return new NotificationListDto(
                repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, LIST_SIZE)).stream()
                        .map(NotificationDto::of)
                        .toList(),
                repository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllRead(userId, Instant.now());
    }
}
