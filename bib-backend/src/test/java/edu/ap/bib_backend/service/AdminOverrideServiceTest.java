package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOverrideServiceTest {

    @Mock
    private TokenStorageService tokenStorageService;

    @Test
    void givenDevModeEnabled_whenEnableOverride_thenStoresOverrideAndReturnsStatus() {
        when(tokenStorageService.hasAdminOverride("user-1")).thenReturn(true);
        when(tokenStorageService.getAdminOverrideTtlSeconds("user-1")).thenReturn(1800L);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        AdminOverrideStatusDto status = service.setOverride("user-1", true, "127.0.0.1");

        verify(tokenStorageService).storeAdminOverride("user-1", 30L);
        verify(tokenStorageService).hasAdminOverride("user-1");
        verify(tokenStorageService).getAdminOverrideTtlSeconds("user-1");

        assertTrue(status.enabled());
        assertEquals(1800L, status.expiresInSeconds());
    }

    @Test
    void givenActiveOverride_whenDisableOverride_thenRemovesOverride() {
        when(tokenStorageService.hasAdminOverride("user-2")).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        AdminOverrideStatusDto status = service.setOverride("user-2", false, "127.0.0.1");

        verify(tokenStorageService).removeAdminOverride("user-2");
        verify(tokenStorageService).hasAdminOverride("user-2");

        assertFalse(status.enabled());
        assertEquals(null, status.expiresInSeconds());
    }

    @Test
    void givenDevModeDisabled_whenGetStatus_thenThrowNotFound() {
        AdminOverrideService service = new AdminOverrideService(tokenStorageService, false, 30L);

        assertThrows(ResponseStatusException.class, () -> service.getStatus("user-3"));

        verify(tokenStorageService, never()).hasAdminOverride("user-3");
    }

    @Test
    void givenDevModeDisabled_whenSetOverride_thenThrowNotFound() {
        AdminOverrideService service = new AdminOverrideService(tokenStorageService, false, 30L);

        assertThrows(ResponseStatusException.class,
                () -> service.setOverride("user-4", true, "127.0.0.1"));

        verify(tokenStorageService, never()).storeAdminOverride("user-4", 30L);
    }

    @Test
    void givenDevModeEnabledAndActiveOverride_whenIsAdminOverrideActive_thenReturnsTrue() {
        when(tokenStorageService.hasAdminOverride("user-5")).thenReturn(true);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        boolean result = service.isAdminOverrideActive("user-5");

        assertTrue(result);
        verify(tokenStorageService).hasAdminOverride("user-5");
    }

    @Test
    void givenDevModeEnabledAndNoOverride_whenIsAdminOverrideActive_thenReturnsFalse() {
        when(tokenStorageService.hasAdminOverride("user-6")).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        boolean result = service.isAdminOverrideActive("user-6");

        assertFalse(result);
        verify(tokenStorageService).hasAdminOverride("user-6");
    }

    @Test
    void givenDevModeDisabled_whenIsAdminOverrideActive_thenReturnsFalseWithoutTokenCheck() {
        AdminOverrideService service = new AdminOverrideService(tokenStorageService, false, 30L);

        boolean result = service.isAdminOverrideActive("user-7");

        assertFalse(result);
        verify(tokenStorageService, never()).hasAdminOverride("user-7");
    }

    @Test
    void givenDevModeEnabledAndActiveOverride_whenGetStatus_thenReturnsStatusWithExpiry() {
        when(tokenStorageService.hasAdminOverride("user-8")).thenReturn(true);
        when(tokenStorageService.getAdminOverrideTtlSeconds("user-8")).thenReturn(600L);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        AdminOverrideStatusDto status = service.getStatus("user-8");

        assertTrue(status.enabled());
        assertEquals(600L, status.expiresInSeconds());
        verify(tokenStorageService).hasAdminOverride("user-8");
        verify(tokenStorageService).getAdminOverrideTtlSeconds("user-8");
    }

    @Test
    void givenDevModeEnabledAndNoOverride_whenGetStatus_thenReturnsInactiveStatus() {
        when(tokenStorageService.hasAdminOverride("user-9")).thenReturn(false);

        AdminOverrideService service = new AdminOverrideService(tokenStorageService, true, 30L);

        AdminOverrideStatusDto status = service.getStatus("user-9");

        assertFalse(status.enabled());
        assertNull(status.expiresInSeconds());
        verify(tokenStorageService).hasAdminOverride("user-9");
        verify(tokenStorageService, never()).getAdminOverrideTtlSeconds("user-9");
    }
}
