package com.pauloneill.arcraidersplanner;

import com.pauloneill.arcraidersplanner.service.MetaforgeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.sync-on-startup", havingValue = "true")
public class StartupRunner implements CommandLineRunner {

    private final MetaforgeSyncService syncService;

    public StartupRunner(MetaforgeSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("--- STARTING DATA SYNC ---");
        try {
            syncService.syncItems(); // Syncs both items AND recipes (includeComponents=true)
            syncService.syncMarkers();
        } catch (Exception e) {
            log.error("Sync failed : ", e);
        }
        log.info("--- SYNC COMPLETE ---");
    }
}