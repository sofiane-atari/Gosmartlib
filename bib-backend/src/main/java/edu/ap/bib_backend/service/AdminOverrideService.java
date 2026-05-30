package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// TEMP_ADMIN_OVERRIDE: remove before production release.
@Service
public class AdminOverrideService {

    private static final Logger log = LoggerFactory.getLogger(AdminOverrideService.class);

    private final TokenStorageService tokenStorageService;
    private final boolean devModeEnabled;
    private final long ttlMinutes;

    public AdminOverrideService(
            TokenStorageService tokenStorageService,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled,
            @Value("${admin.override.ttl-minutes:30}") long ttlMinutes) {
        this.tokenStorageService = tokenStorageService;
        this.devModeEnabled = devModeEnabled;
        this.ttlMinutes = ttlMinutes;
    }

    public AdminOverrideStatusDto setOverride(String userId, boolean shouldEnable, String remoteAddress) {
        ensureFeatureAvailable();

        if (shouldEnable) {
            tokenStorageService.storeAdminOverride(userId, ttlMinutes);
        } else {
            tokenStorageService.removeAdminOverride(userId);
        }

        log.info(
                "TEMP_ADMIN_OVERRIDE action={} userId={} remoteAddress={} ttlMinutes={}",
                shouldEnable ? "ENABLE" : "DISABLE",
                userId,
                remoteAddress,
                ttlMinutes);

        return getStatus(userId);
    }

    public AdminOverrideStatusDto getStatus(String userId) {
        ensureFeatureAvailable();
        boolean active = tokenStorageService.hasAdminOverride(userId);
        Long expiresInSeconds = active ? tokenStorageService.getAdminOverrideTtlSeconds(userId) : null;
        return new AdminOverrideStatusDto(active, expiresInSeconds);
    }

    public boolean isAdminOverrideActive(String userId) {
        if (!isFeatureAvailable()) {
            return false;
        }
        return tokenStorageService.hasAdminOverride(userId);
    }

    private void ensureFeatureAvailable() {
        if (!isFeatureAvailable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin override is unavailable");
        }
    }

    private boolean isFeatureAvailable() {
        return devModeEnabled;
    }
}
