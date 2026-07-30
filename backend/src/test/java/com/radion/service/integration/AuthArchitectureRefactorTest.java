package com.radion.service.integration;

import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.dto.ConnectionDTO;
import com.radion.dto.DashboardSummaryDTO;
import com.radion.repository.*;
import com.radion.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthArchitectureRefactorTest {

    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AIProcessingLogRepository aiLogRepository;
    @Mock private ConnectedServiceRepository connectedServiceRepository;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private UUID testUserId;
    private User websiteUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        // Website Identity (NextAuth user)
        websiteUser = User.builder()
                .id(testUserId)
                .email("ayaan.personal@gmail.com")
                .firstName("Ayaan")
                .lastName("Personal")
                .avatarUrl("https://lh3.googleusercontent.com/personal-avatar")
                .build();
    }

    @Test
    void testIndependentAccountIdentityPerService() {
        // Arrange: User connects 3 different Google accounts for 3 different services
        ConnectedService gmailConn = ConnectedService.builder()
                .id(UUID.randomUUID())
                .user(websiteUser)
                .platform(Platform.GMAIL)
                .status(ConnectionStatus.CONNECTED)
                .accountEmail("placements@gmail.com")
                .accountName("Placement Cell Account")
                .accountAvatarUrl("https://lh3.googleusercontent.com/placements-avatar")
                .lastSyncAt(LocalDateTime.now().minusMinutes(5))
                .build();

        ConnectedService classroomConn = ConnectedService.builder()
                .id(UUID.randomUUID())
                .user(websiteUser)
                .platform(Platform.CLASSROOM)
                .status(ConnectionStatus.CONNECTED)
                .accountEmail("student@coer.edu.in")
                .accountName("COER Student Account")
                .accountAvatarUrl("https://lh3.googleusercontent.com/coer-avatar")
                .lastSyncAt(LocalDateTime.now().minusHours(1))
                .build();

        List<ConnectedService> userConnections = Arrays.asList(gmailConn, classroomConn);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(websiteUser));
        when(eventRepository.findByUserIdAndEventTimeBetweenOrderByEventTimeAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByUserIdAndEventTimeAfterOrderByEventTimeAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(aiLogRepository.findRecentPlacementLogs(eq(testUserId), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(connectedServiceRepository.findByUserId(testUserId))
                .thenReturn(userConnections);
        when(notificationRepository.countByUserIdAndIsReadFalse(testUserId))
                .thenReturn(0);

        // Act
        DashboardSummaryDTO summary = dashboardService.getDashboardSummary(testUserId, null);

        // Assert 1: Website identity remains unchanged and strictly independent
        assertNotNull(summary.getUser());
        assertEquals("Ayaan", summary.getUser().getFirstName());
        assertEquals("https://lh3.googleusercontent.com/personal-avatar", summary.getUser().getAvatarUrl());

        // Assert 2: All 2 connected services are returned with their distinct account emails and names
        assertEquals(2, summary.getConnections().size());

        ConnectionDTO gmailDto = summary.getConnections().stream()
                .filter(c -> c.getPlatform() == Platform.GMAIL).findFirst().orElseThrow();
        assertEquals("placements@gmail.com", gmailDto.getAccountEmail());
        assertEquals("Placement Cell Account", gmailDto.getAccountName());
        assertEquals("CONNECTED", gmailDto.getStatus());
        assertNotEquals(websiteUser.getEmail(), gmailDto.getAccountEmail());



        ConnectionDTO classroomDto = summary.getConnections().stream()
                .filter(c -> c.getPlatform() == Platform.CLASSROOM).findFirst().orElseThrow();
        assertEquals("student@coer.edu.in", classroomDto.getAccountEmail());
        assertEquals("COER Student Account", classroomDto.getAccountName());
    }
}
