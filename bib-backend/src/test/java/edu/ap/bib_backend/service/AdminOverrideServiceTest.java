package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOverrideServiceTest {

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private Environment environment;

    @Test
    void givenDevProfileAndEnabledFlag_whenEnableOverride_thenStoresOverrideAndReturnsStatus() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(tokenStorageService.hasAdminOverride("user-1")).thenReturn(true);
        when(tokenStorageService.getAdminOverrideTtlSeconds("user-1")).thenReturn(1800L);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        AdminOverrideStatusDto status = service.setOverride("user-1", true, "127.0.0.1");

        verify(tokenStorageService).storeAdminOverride("user-1", 30);
        assertTrue(status.enabled());
        assertEquals(1800L, status.expiresInSeconds());
    }

    @Test
    void givenActiveOverride_whenDisableOverride_thenRemovesOverride() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(tokenStorageService.hasAdminOverride("user-2")).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        AdminOverrideStatusDto status = service.setOverride("user-2", false, "127.0.0.1");

        verify(tokenStorageService).removeAdminOverride("user-2");
        assertFalse(status.enabled());
    }

    @Test
    void givenProdProfile_whenCheckingStatus_thenThrowNotFound() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        assertThrows(ResponseStatusException.class, () -> service.getStatus("user-3"));
        verify(tokenStorageService, never()).hasAdminOverride("user-3");
    }

    @Test
    void givenLocalhostOnlyAndRemoteIp_whenEnableOverride_thenThrowForbidden() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, true);

        assertThrows(ResponseStatusException.class, () -> service.setOverride("user-4", true, "192.168.0.8"));
        verify(tokenStorageService, never()).storeAdminOverride("user-4", 30);
    }
}

