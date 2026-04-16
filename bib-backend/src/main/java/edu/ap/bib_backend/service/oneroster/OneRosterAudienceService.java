package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.dto.OneRosterAudienceOptionsDto;
import edu.ap.bib_backend.service.TokenStorageService;
import org.springframework.stereotype.Service;

@Service
public class OneRosterAudienceService {

    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;

    public OneRosterAudienceService(OneRosterClient oneRosterClient,
            TokenStorageService tokenStorageService) {
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
    }

    public OneRosterAudienceOptionsDto getAudienceOptions(String userId, String schoolCode) {
        String smartschoolAccessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        OneRosterClient.AudiencePayload payload = oneRosterClient
                .fetchAudiencePayloadWithBearer(schoolCode, smartschoolAccessToken);

        return new OneRosterAudienceOptionsDto(payload.classes(), payload.years(), payload.schools());
    }
}
