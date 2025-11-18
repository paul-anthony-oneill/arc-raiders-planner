package com.pauloneill.arcraidersplanner; // Adjust package if needed

import com.pauloneill.arcraidersplanner.service.MetaforgeSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MetaforgeSyncService syncService;

    public StartupRunner(MetaforgeSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- STARTING DATA SYNC ---");
        try {
            syncService.syncItems();
        } catch (Exception e) {
            System.out.println("Sync failed : " + e.getMessage());
        }
        System.out.println("--- SYNC COMPLETE ---");
    }
}