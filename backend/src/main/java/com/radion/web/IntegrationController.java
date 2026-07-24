package com.radion.web;

import com.radion.service.integration.SyncManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final SyncManagerService syncManagerService;

    @PostMapping("/sync")
    public ResponseEntity<Void> syncNow(
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000000") UUID userId) {
        syncManagerService.triggerManualSync(userId);
        return ResponseEntity.ok().build();
    }
}