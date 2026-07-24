package com.radion.web;

import com.radion.domain.models.Notification;
import com.radion.dto.NotificationDTO;
import com.radion.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000000") UUID userId) {
        
        // Fetch all notifications for user, sorted by newest first (assuming a custom query or sorting in memory for this example)
        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        List<NotificationDTO> dtos = notifications.stream().map(n -> NotificationDTO.builder()
                .id(n.getId().toString())
                .title(n.getTitle())
                .content(n.getContent())
                .timestamp(formatRelativeTime(n.getCreatedAt()))
                .isRead(n.isRead())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok().build();
    }

    private String formatRelativeTime(LocalDateTime time) {
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 60) return minutes + "m ago";
        return (minutes / 60) + "h ago";
    }
}