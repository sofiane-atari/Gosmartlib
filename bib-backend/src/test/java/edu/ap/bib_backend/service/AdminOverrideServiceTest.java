package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    void givenNonProdProfileAndEnabledFlag_whenEnableOverride_thenStoresOverrideAndReturnsStatus() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(tokenStorageService.hasAdminOverride("user-1")).thenReturn(true);
        when(tokenStorageService.getAdminOverrideTtlSeconds("user-1")).thenReturn(1800L);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        AdminOverrideStatusDto status = service.setOverride("user-1", true, "127.0.0.1");

        verify(tokenStorageService).storeAdminOverride("user-1", 30);
        verify(tokenStorageService).hasAdminOverride("user-1");
        verify(tokenStorageService).getAdminOverrideTtlSeconds("user-1");

        assertTrue(status.enabled());
        assertEquals(1800L, status.expiresInSeconds());
    }

    @Test
    void givenActiveOverride_whenDisableOverride_thenRemovesOverride() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(tokenStorageService.hasAdminOverride("user-2")).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        AdminOverrideStatusDto status = service.setOverride("user-2", false, "127.0.0.1");

        verify(tokenStorageService).removeAdminOverride("user-2");
        verify(tokenStorageService).hasAdminOverride("user-2");

        assertFalse(status.enabled());
        assertEquals(null, status.expiresInSeconds());
    }

    @Test
    void givenProdProfile_whenCheckingStatus_thenThrowNotFound() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, false);

        assertThrows(ResponseStatusException.class, () -> service.getStatus("user-3"));

        verify(tokenStorageService, never()).hasAdminOverride("user-3");
    }

    @Test
    void givenDisabledFeature_whenCheckingStatus_thenThrowNotFound() {
        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, false, 30, false);

        assertThrows(ResponseStatusException.class, () -> service.getStatus("user-5"));

        verify(tokenStorageService, never()).hasAdminOverride("user-5");
    }

    @Test
    void givenLocalhostOnlyAndRemoteIp_whenEnableOverride_thenThrowForbidden() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, true);

        assertThrows(
                ResponseStatusException.class,
                () -> service.setOverride("user-4", true, "192.168.0.8"));

        verify(tokenStorageService, never()).storeAdminOverride("user-4", 30);
    }

    @Test
    void givenLocalhostOnlyAndLocalhostIp_whenEnableOverride_thenStoresOverride() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(tokenStorageService.hasAdminOverride("user-6")).thenReturn(true);
        when(tokenStorageService.getAdminOverrideTtlSeconds("user-6")).thenReturn(1800L);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, environment, true, 30, true);

        AdminOverrideStatusDto status = service.setOverride("user-6", true, "127.0.0.1");

        verify(tokenStorageService).storeAdminOverride("user-6", 30);

        assertTrue(status.enabled());
        assertEquals(1800L, status.expiresInSeconds());
    }
}