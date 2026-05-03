package edu.ap.bib_backend.service.oneroster;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.service.oneroster.OneRosterClient;

@Service
public class ExternalUserDisplayService {

    private final OneRosterClient oneRosterClient;

    public ExternalUserDisplayService(OneRosterClient oneRosterClient) {
        this.oneRosterClient = oneRosterClient;
    }

    public Map<String, String> getDisplayNames(String schoolCode, Set<String> userIds) {
        if (schoolCode == null || schoolCode.isBlank() || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        return oneRosterClient.fetchUserDisplayNames(schoolCode, userIds);
    }

    public String getDisplayName(String schoolCode, String userId) {
        if (userId == null || userId.isBlank()) {
            return "Onbekende gebruiker";
        }

        return getDisplayNames(schoolCode, Set.of(userId))
                .getOrDefault(userId, "Onbekende gebruiker");
    }
}