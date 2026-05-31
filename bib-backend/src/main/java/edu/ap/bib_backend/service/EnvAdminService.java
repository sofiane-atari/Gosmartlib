package edu.ap.bib_backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class EnvAdminService {

    private static final Logger log = LoggerFactory.getLogger(EnvAdminService.class);

    private final List<String> adminUsernames;
    private final List<String> adminSchoolDomains;

    public EnvAdminService(
            @Value("${app.admin-usernames:}") String adminUsernamesRaw,
            @Value("${app.admin-school-domains:}") String adminSchoolDomainsRaw) {
        this.adminUsernames = parseCommaSeparated(adminUsernamesRaw);
        this.adminSchoolDomains = parseCommaSeparated(adminSchoolDomainsRaw);
    }

    @PostConstruct
    void validate() {
        if (adminUsernames.isEmpty() && adminSchoolDomains.isEmpty()) {
            log.info("No env-var admin accounts configured.");
            return;
        }

        if (adminUsernames.size() != adminSchoolDomains.size()) {
            log.warn(
                    "ADMIN_USERNAMES ({} entries) and ADMIN_SCHOOL_DOMAINS ({} entries) have different lengths. "
                            + "Only matching indices will be used.",
                    adminUsernames.size(),
                    adminSchoolDomains.size());
        }

        log.info("Loaded {} env-var admin account(s) from environment.", adminUsernames.size());
    }

    public boolean isEnvAdmin(String userId, String schoolDomain) {
        if (userId == null || schoolDomain == null) {
            return false;
        }

        for (int i = 0; i < adminUsernames.size() && i < adminSchoolDomains.size(); i++) {
            if (adminUsernames.get(i).equals(userId)
                    && adminSchoolDomains.get(i).equalsIgnoreCase(schoolDomain)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEnvAdminByUserId(String userId) {
        if (userId == null) {
            return false;
        }

        return adminUsernames.contains(userId);
    }

    public List<String> getAdminUsernames() {
        return Collections.unmodifiableList(adminUsernames);
    }

    private List<String> parseCommaSeparated(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }
}
