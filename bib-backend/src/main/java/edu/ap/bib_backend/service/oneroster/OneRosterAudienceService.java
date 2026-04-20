package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.dto.OneRosterAudienceOptionsDto;
import edu.ap.bib_backend.service.TokenStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OneRosterAudienceService {

    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;

    public OneRosterAudienceService(OneRosterClient oneRosterClient,
            TokenStorageService tokenStorageService) {
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
    }

    public OneRosterAudienceOptionsDto getAudienceOptions(String userId, String schoolCode, String classCode) {
        String smartschoolAccessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        OneRosterClient.AudiencePayload payload = oneRosterClient
                .fetchAudiencePayloadWithBearer(schoolCode, smartschoolAccessToken);

        List<String> classes = withFallbackOption(payload.classes(), classCode);
        return new OneRosterAudienceOptionsDto(classes, payload.years(), payload.schools());
    }

    private List<String> withFallbackOption(List<String> options, String fallbackOption) {
        Set<String> deduped = new LinkedHashSet<>();

        if (options != null) {
            for (String option : options) {
                if (StringUtils.hasText(option)) {
                    deduped.add(option.trim());
                }
            }
        }

        if (StringUtils.hasText(fallbackOption)) {
            deduped.add(fallbackOption.trim());
        }

        List<String> result = new ArrayList<>(deduped);
        result.sort(Comparator.comparing(String::toLowerCase));
        return result;
    }
}
