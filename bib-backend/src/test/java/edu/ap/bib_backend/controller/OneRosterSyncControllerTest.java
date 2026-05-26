package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.SyncConfigDto;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OneRosterSyncControllerTest {

    private OneRosterSyncController controller;
    private OneRosterUserSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = mock(OneRosterUserSyncService.class);
        controller = new OneRosterSyncController(syncService, "0 0 3 * * *", "Europe/Brussels", 1);
    }

    @Test
    void Given_SyncRequested_When_SyncUsersNow_Then_CallsSyncAllSchools() {
        controller.syncUsersNow();

        verify(syncService).syncAllSchools();
    }

    @Test
    void Given_DefaultConfig_When_GetSyncConfig_Then_ReturnsConfigDto() {
        ResponseEntity<SyncConfigDto> response = controller.getSyncConfig();
        SyncConfigDto result = response.getBody();

        assertNotNull(result);
        assertEquals("03:00", result.syncTime());
        assertEquals("Europe/Brussels", result.syncZone());
        assertEquals(1, result.syncIntervalDays());
        assertEquals("0 0 3 * * *", result.syncCron());
    }

    @Test
    void Given_CustomConfig_When_GetSyncConfig_Then_ReturnsCustomConfigDto() {
        OneRosterSyncController customController = new OneRosterSyncController(
                syncService, "0 30 2 * * *", "Europe/London", 2);

        ResponseEntity<SyncConfigDto> response = customController.getSyncConfig();
        SyncConfigDto result = response.getBody();

        assertNotNull(result);
        assertEquals("02:30", result.syncTime());
        assertEquals("Europe/London", result.syncZone());
        assertEquals(2, result.syncIntervalDays());
    }

    @Test
    void Given_InvalidCron_When_GetSyncConfig_Then_SyncTimeIsNull() {
        OneRosterSyncController invalidCronController = new OneRosterSyncController(
                syncService, "invalid cron", "UTC", 1);

        ResponseEntity<SyncConfigDto> response = invalidCronController.getSyncConfig();
        SyncConfigDto result = response.getBody();

        assertNotNull(result);
        assertNull(result.syncTime());
    }

    @Test
    void Given_ShortCron_When_GetSyncConfig_Then_SyncTimeIsNull() {
        OneRosterSyncController shortCronController = new OneRosterSyncController(
                syncService, "0 0", "UTC", 1);

        ResponseEntity<SyncConfigDto> response = shortCronController.getSyncConfig();
        SyncConfigDto result = response.getBody();

        assertNotNull(result);
        assertNull(result.syncTime());
    }

    @Test
    void Given_MalformedCronParts_When_GetSyncConfig_Then_SyncTimeIsNull() {
        OneRosterSyncController malformedCronController = new OneRosterSyncController(
                syncService, "0 x 3 * * *", "UTC", 1);

        ResponseEntity<SyncConfigDto> response = malformedCronController.getSyncConfig();
        SyncConfigDto result = response.getBody();

        assertNotNull(result);
        assertNull(result.syncTime());
    }
}
