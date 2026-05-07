package edu.ap.bib_backend.service.oneroster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OneRosterUserSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterUserSyncScheduler.class);

    private final OneRosterUserSyncService syncService;

    public OneRosterUserSyncScheduler(OneRosterUserSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${oneroster.sync.cron:0 0 3 * * *}", zone = "${oneroster.sync.zone:Europe/Brussels}")
    public void syncUsersDaily() {
        logger.info("Start OneRoster daily user sync");
        syncService.syncAllSchools();
        logger.info("Finished OneRoster daily user sync");
    }
}
