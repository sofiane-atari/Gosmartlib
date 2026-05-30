package edu.ap.bib_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.SyncConfigDto;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;

@RestController
@RequestMapping("/oneroster")
public class OneRosterSyncController {

    private final OneRosterUserSyncService syncService;
    private final String syncCron;
    private final String syncZone;
    private final int syncIntervalDays;

    public OneRosterSyncController(
            OneRosterUserSyncService syncService,
            @Value("${oneroster.sync.cron:0 0 3 * * *}") String syncCron,
            @Value("${oneroster.sync.zone:Europe/Brussels}") String syncZone,
            @Value("${oneroster.sync.interval-days:1}") int syncIntervalDays) {
        this.syncService = syncService;
        this.syncCron = syncCron;
        this.syncZone = syncZone;
        this.syncIntervalDays = syncIntervalDays;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> syncUsersNow() {
        syncService.syncAllSchools();
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the configured OneRoster sync schedule.
     *
     * @return sync configuration response
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SyncConfigDto> getSyncConfig() {
        String syncTime = extractTime(syncCron);
        return ResponseEntity.ok(new SyncConfigDto(syncTime, syncZone, syncIntervalDays, syncCron));
    }

    /**
     * Extracts HH:mm time from a cron expression.
     *
     * @param cron cron expression
     * @return formatted time, or null when unavailable
     */
    private static String extractTime(String cron) {
        String[] parts = cron.split("\\s+");
        if (parts.length >= 3) {
            try {
                int hour = Integer.parseInt(parts[2]);
                int minute = Integer.parseInt(parts[1]);
                return String.format("%02d:%02d", hour, minute);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
