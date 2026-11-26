package com.educa.backend.notification;

import java.util.List;

/** Les dernières notifications de l'utilisateur et le nombre de non lues (pastille de la cloche). */
public record NotificationListDto(List<NotificationDto> items, long unreadCount) {
}
