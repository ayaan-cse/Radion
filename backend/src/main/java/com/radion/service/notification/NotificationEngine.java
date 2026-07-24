package com.radion.service.notification;

import com.radion.domain.models.User;
import com.radion.service.notification.strategy.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationEngine {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationEngine(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, Function.identity()));
    }

    public void dispatch(User user, String title, String message, List<NotificationChannel> channels) {
        for (NotificationChannel channel : channels) {
            NotificationSender sender = senders.get(channel);
            if (sender != null) {
                try {
                    sender.send(user, title, message, null);
                } catch (Exception e) {
                    log.error("Failed to send {} notification to user {}", channel, user.getId(), e);
                }
            }
        }
    }
}