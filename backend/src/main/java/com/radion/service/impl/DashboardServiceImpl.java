package com.radion.service.impl;

import com.radion.domain.models.*;
import com.radion.dto.*;
import com.radion.repository.*;
import com.radion.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final AIProcessingLogRepository aiLogRepository;
    private final ConnectedServiceRepository connectedServiceRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(UUID userId, String searchQuery) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        // Fetch Data
        List<Event> todaysEvents = eventRepository
                .findByUserIdAndEventTimeBetweenOrderByEventTimeAsc(userId, startOfDay, endOfDay);

        List<Event> upcomingEvents = eventRepository
                .findByUserIdAndEventTimeAfterOrderByEventTimeAsc(userId, endOfDay);

        List<AIProcessingLog> recentLogs = aiLogRepository
                .findTop5ByMessageUserIdOrderByProcessedAtDesc(userId);

        // ---------------- Search Filter ----------------
        if (StringUtils.hasText(searchQuery)) {

            String query = searchQuery.toLowerCase();

            todaysEvents = todaysEvents.stream()
                    .filter(e -> matchesSearch(e, query))
                    .collect(Collectors.toList());

            upcomingEvents = upcomingEvents.stream()
                    .filter(e -> matchesSearch(e, query))
                    .collect(Collectors.toList());

            recentLogs = recentLogs.stream()
                    .filter(l ->
                            l.getMessage().getTitle().toLowerCase().contains(query)
                                    || l.getAiSummary().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        List<ConnectedService> connections =
                connectedServiceRepository.findByUserId(userId);

        int unreadCount =
                notificationRepository.countByUserIdAndIsReadFalse(userId);

        // Calculate Last Sync
        String lastSync = connections.stream()
                .map(ConnectedService::getLastSyncAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(this::formatRelativeTime)
                .orElse("Never");

        // ---------------- Analytics ----------------
        long totalAutomated = eventRepository.count();

        double avgConfidence = recentLogs.stream()
                .mapToDouble(AIProcessingLog::getConfidenceScore)
                .average()
                .orElse(0.95);

        DashboardSummaryDTO.AnalyticsDTO analytics =
                DashboardSummaryDTO.AnalyticsDTO.builder()
                        .totalEventsAutomated((int) totalAutomated)
                        .tasksPending(3)
                        .averageAiConfidence(
                                Math.round(avgConfidence * 100.0) / 100.0
                        )
                        .hoursSaved((int) (totalAutomated * 0.25))
                        .build();

        return DashboardSummaryDTO.builder()
                .user(DashboardSummaryDTO.UserDTO.builder()
                        .firstName(user.getFirstName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .lastSyncTime(lastSync)
                .unreadNotifications(unreadCount)
                .connections(
                        connections.stream()
                                .map(this::mapConnection)
                                .collect(Collectors.toList())
                )
                .todaysEvents(
                        todaysEvents.stream()
                                .map(this::mapTodayEvent)
                                .collect(Collectors.toList())
                )
                .upcomingEvents(
                        upcomingEvents.stream()
                                .limit(5)
                                .map(this::mapUpcomingEvent)
                                .collect(Collectors.toList())
                )
                .recentMessages(
                        recentLogs.stream()
                                .map(this::mapRecentMessage)
                                .collect(Collectors.toList())
                )
                .analytics(analytics)
                .build();
    }

    private ConnectionDTO mapConnection(ConnectedService c) {
        return ConnectionDTO.builder()
                .platform(c.getPlatform())
                .status(c.getStatus().name())
                .build();
    }

    private EventDTO mapTodayEvent(Event e) {
        return EventDTO.builder()
                .id(e.getId().toString())
                .time(e.getEventTime().format(DateTimeFormatter.ofPattern("hh:mm a")))
                .title(e.getTitle())
                .source(e.getCompanyOrSource())
                .category(e.getCategory())
                .build();
    }

    private UpcomingEventDTO mapUpcomingEvent(Event e) {
        return UpcomingEventDTO.builder()
                .id(e.getId().toString())
                .day(String.valueOf(e.getEventTime().getDayOfMonth()))
                .month(e.getEventTime().format(DateTimeFormatter.ofPattern("MMM")))
                .company(e.getCompanyOrSource())
                .title(e.getTitle())
                .time(e.getEventTime().format(DateTimeFormatter.ofPattern("hh:mm a")))
                .category(e.getCategory())
                .build();
    }

    private MessageDTO mapRecentMessage(AIProcessingLog log) {

        Message msg = log.getMessage();

        return MessageDTO.builder()
                .id(msg.getId().toString())
                .platform(msg.getPlatform())
                .title(msg.getTitle())
                .summary(log.getAiSummary())
                .timestamp(formatRelativeTime(log.getProcessedAt()))
                .isUnread(msg.isUnread())
                .build();
    }

    private boolean matchesSearch(Event e, String query) {
        return e.getTitle().toLowerCase().contains(query)
                || (e.getCompanyOrSource() != null
                && e.getCompanyOrSource().toLowerCase().contains(query));
    }

    private String formatRelativeTime(LocalDateTime time) {

        long minutes = java.time.Duration
                .between(time, LocalDateTime.now())
                .toMinutes();

        if (minutes < 1)
            return "Just now";

        if (minutes < 60)
            return minutes + "m ago";

        long hours = minutes / 60;

        if (hours < 24)
            return hours + "h ago";

        return (hours / 24) + "d ago";
    }
}