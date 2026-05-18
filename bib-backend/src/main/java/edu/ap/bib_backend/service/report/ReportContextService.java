package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.GroupInfoItemDTO;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ReportContextService {

    private final TokenStorageService tokenStorageService;
    private final SmartschoolProfileService smartschoolProfileService;

    public ReportContextService(
            TokenStorageService tokenStorageService,
            SmartschoolProfileService smartschoolProfileService) {
        this.tokenStorageService = tokenStorageService;
        this.smartschoolProfileService = smartschoolProfileService;
    }

    public ReportContext getContext(Authentication authentication) {
        String userId = authentication.getName();
        String accessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Geen Smartschool access token gevonden");
        }

        SmartschoolProfileDto profile = smartschoolProfileService.fetchProfile(accessToken);

        if (profile == null || profile.platform() == null || profile.platform().isBlank()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Geen Smartschool platform gevonden");
        }

        String schoolCode = extractHostFromPlatform(profile.platform());

        return new ReportContext(
                accessToken,
                profile.platform(),
                schoolCode);
    }

    public List<String> getSmartschoolGroupNames(String accessToken, String platform) {
        SmartschoolGroupInfoDTO groupInfo = smartschoolProfileService.fetchGroupInfo(
                accessToken,
                platform);

        if (groupInfo == null || groupInfo.groups() == null) {
            return List.of();
        }

        return groupInfo.groups()
                .stream()
                .filter(Objects::nonNull)
                .map(GroupInfoItemDTO::name)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .filter(name -> !name.equalsIgnoreCase("Leerlingen"))
                .filter(name -> !name.equalsIgnoreCase("Leerkrachten"))
                .filter(name -> !name.equalsIgnoreCase("Personeel"))
                .distinct()
                .toList();
    }

    public String extractClassCodeFromAuthentication(Authentication authentication) {
        Object details = authentication.getDetails();

        if (details instanceof java.util.Map<?, ?> detailsMap) {
            Object classCode = detailsMap.get("classCode");

            if (classCode instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }

        return null;
    }

    private String extractHostFromPlatform(String platform) {
        try {
            URI uri = URI.create(platform);

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException();
            }

            return uri.getHost();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Geen geldige Smartschool platform URL gevonden");
        }
    }

    public record ReportContext(
            String accessToken,
            String platform,
            String schoolCode) {
    }
}