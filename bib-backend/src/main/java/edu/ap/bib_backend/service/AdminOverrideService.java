package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// TEMP_ADMIN_OVERRIDE: remove before production release.
@Service
public class AdminOverrideService {

    private static final Logger log = LoggerFactory.getLogger(AdminOverrideService.class);

    private final TokenStorageService tokenStorageService;
    private final Environment environment;
    private final boolean enabled;
    private final long ttlMinutes;
    private final boolean localhostOnly;

    public AdminOverrideService(
            TokenStorageService tokenStorageService,
            Environment environment,
            @Value("${admin.override.enabled:false}") boolean enabled,
            @Value("${admin.override.ttl-minutes:30}") long ttlMinutes,
            @Value("${admin.override.localhost-only:false}") boolean localhostOnly) {
        this.tokenStorageService = tokenStorageService;
        this.environment = environment;
        this.enabled = enabled;
        this.ttlMinutes = ttlMinutes;
        this.localhostOnly = localhostOnly;
    }

    public AdminOverrideStatusDto setOverride(String userId, boolean shouldEnable, String remoteAddress) {
        ensureFeatureAvailable();
        ensureNetworkAllowed(remoteAddress);

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
        return enabled && !environment.acceptsProfiles(Profiles.of("prod"));
    }

    private void ensureNetworkAllowed(String remoteAddress) {
        if (!localhostOnly) {
            return;
        }

        if (remoteAddress == null || remoteAddress.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin override requires localhost");
        }

        if (!"127.0.0.1".equals(remoteAddress) && !"0:0:0:0:0:0:0:1".equals(remoteAddress)
                && !"::1".equals(remoteAddress)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin override requires localhost");
        }
    }
}
