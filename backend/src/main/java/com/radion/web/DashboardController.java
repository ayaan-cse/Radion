package com.radion.web;

import com.radion.dto.DashboardSummaryDTO;
import com.radion.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000000") UUID userId,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                dashboardService.getDashboardSummary(userId, search)
        );
    }
}