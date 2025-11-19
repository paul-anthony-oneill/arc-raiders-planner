package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.service.MetaforgeSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MetaforgeSyncService syncService;

    @Value("${app.admin.api-key}")
    private String adminApiKey;

    public AdminController(MetaforgeSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Endpoint: POST /api/admin/sync
     * Triggers a full data refresh from the Metaforge API.
     * Requires an API key in the header for basic security.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync(
            @RequestHeader("X-API-KEY") String apiKey) {

        if (!adminApiKey.equals(apiKey)) {
            return new ResponseEntity<>("Invalid API Key", HttpStatus.UNAUTHORIZED);
        }

        try {
            syncService.syncItems();
            return ResponseEntity.ok("Data synchronization completed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Synchronization failed: " + e.getMessage());
        }
    }
}