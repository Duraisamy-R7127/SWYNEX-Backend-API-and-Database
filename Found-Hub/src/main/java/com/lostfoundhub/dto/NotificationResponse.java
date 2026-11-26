package com.lostfoundhub.dto;

import com.lostfoundhub.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String message, boolean read, LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
