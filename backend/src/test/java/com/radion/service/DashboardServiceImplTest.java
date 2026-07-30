package com.radion.service;

import com.radion.domain.models.User;
import com.radion.dto.DashboardSummaryDTO;
import com.radion.repository.*;
import com.radion.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AIProcessingLogRepository aiLogRepository;
    @Mock private ConnectedServiceRepository connectedServiceRepository;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder().id(testUserId).firstName("Test").build();
    }

    @Test
    void getDashboardSummary_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findByUserIdAndEventTimeBetweenOrderByEventTimeAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByUserIdAndEventTimeAfterOrderByEventTimeAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(aiLogRepository.findRecentPlacementLogs(eq(testUserId), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(connectedServiceRepository.findByUserId(testUserId))
                .thenReturn(Collections.emptyList());
        when(notificationRepository.countByUserIdAndIsReadFalse(testUserId))
                .thenReturn(5);

        // Act
        DashboardSummaryDTO result = dashboardService.getDashboardSummary(testUserId, null);

        // Assert
        assertNotNull(result);
        assertEquals("Test", result.getUser().getFirstName());
        assertEquals(5, result.getUnreadNotifications());
        assertTrue(result.getTodaysEvents().isEmpty());
        assertNotNull(result.getAnalytics());
    }

    @Test
    void getDashboardSummary_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> dashboardService.getDashboardSummary(testUserId, null));
    }
}