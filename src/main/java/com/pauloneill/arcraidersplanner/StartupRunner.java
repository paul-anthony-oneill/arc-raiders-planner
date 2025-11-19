package com.pauloneill.arcraidersplanner;

import com.pauloneill.arcraidersplanner.service.MetaforgeSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final MetaforgeSyncService syncService;
    @Value("${app.data.sync-on-startup:false}")
    private boolean syncOnStartup;

    public StartupRunner(MetaforgeSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!syncOnStartup) {
            System.out.println("--- STARTING DATA SYNC ---");
            try {
                syncService.syncItems();
            } catch (Exception e) {
                System.out.println("Sync failed : " + e.getMessage());
            }
            System.out.println("--- SYNC COMPLETE ---");
        } else {
            System.out.println("Data sync skipped. Run POST /api/admin/sync to update.");
        }
    }
}