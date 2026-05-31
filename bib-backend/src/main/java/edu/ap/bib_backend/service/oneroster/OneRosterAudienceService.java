package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.dto.OneRosterAudienceOptionsDto;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.TokenStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OneRosterAudienceService {

    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;
    private final ActiveSchoolContext activeSchoolContext;

    public OneRosterAudienceService(OneRosterClient oneRosterClient,
            TokenStorageService tokenStorageService,
            ActiveSchoolContext activeSchoolContext) {
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
        this.activeSchoolContext = activeSchoolContext;
    }

    public OneRosterAudienceOptionsDto getAudienceOptions(String userId, String schoolCode, String classCode) {
        String smartschoolAccessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        OneRosterClient.AudiencePayload payload = oneRosterClient
                .fetchAudiencePayloadWithBearer(schoolCode, smartschoolAccessToken);

        List<String> classes = withFallbackOption(payload.classes(), classCode);

        if (activeSchoolContext.isClassOverrideActive()) {
            List<String> activeCodes = activeSchoolContext.getActiveClassCodes();
            List<String> activeNames = activeSchoolContext.getActiveClassNames();
            classes = classes.stream()
                    .filter(c -> matchesActiveClass(c, activeCodes, activeNames))
                    .toList();
        }

        return new OneRosterAudienceOptionsDto(classes, payload.years(), payload.schools());
    }

    private boolean matchesActiveClass(String candidate, List<String> activeCodes, List<String> activeNames) {
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        return activeCodes.stream().anyMatch(c -> c.trim().equalsIgnoreCase(normalized))
                || activeNames.stream().anyMatch(n -> n.trim().equalsIgnoreCase(normalized));
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
