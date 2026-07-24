package com.radion.service;

import com.radion.dto.DashboardSummaryDTO;
import java.util.UUID;

public interface DashboardService {
    DashboardSummaryDTO getDashboardSummary(UUID userId);
}