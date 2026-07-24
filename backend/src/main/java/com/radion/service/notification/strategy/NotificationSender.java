package com.radion.service.notification.strategy;

import com.radion.domain.models.User;
import com.radion.service.notification.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();
    void send(User user, String title, String message, String actionUrl);
}