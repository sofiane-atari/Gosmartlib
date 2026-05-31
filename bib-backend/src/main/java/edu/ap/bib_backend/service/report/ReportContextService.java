package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.GroupInfoItemDTO;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ReportContextService {

    private final TokenStorageService tokenStorageService;
    private final SmartschoolProfileService smartschoolProfileService;
    private final CurrentUserService currentUserService;

    public ReportContextService(
            TokenStorageService tokenStorageService,
            SmartschoolProfileService smartschoolProfileService,
            CurrentUserService currentUserService) {
        this.tokenStorageService = tokenStorageService;
        this.smartschoolProfileService = smartschoolProfileService;
        this.currentUserService = currentUserService;
    }

    /**
     * Resolves Smartschool token, platform, and school code for the user.
     *
     * @param authentication current authenticated user
     * @return report context for OneRoster and Smartschool calls
     */
    public ReportContext getContext(Authentication authentication) {
        String userId = authentication.getName();
        String accessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Geen Smartschool access token gevonden");
        }

        School activeSchool = currentUserService.getCurrentUserSchool(authentication);
        String schoolCode = activeSchool.getDomainName();

        SmartschoolProfileDto profile = smartschoolProfileService.fetchProfile(accessToken);

        if (profile == null || profile.platform() == null || profile.platform().isBlank()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Geen Smartschool platform gevonden");
        }

        return new ReportContext(
                accessToken,
                profile.platform(),
                schoolCode);
    }

    /**
     * Fetches non-generic Smartschool group names for a platform.
     *
     * @param accessToken Smartschool access token
     * @param platform Smartschool platform URL
     * @return distinct group names relevant for class matching
     */
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

    /**
     * Reads a class code from authentication details when present.
     *
     * @param authentication current authenticated user
     * @return trimmed class code, or null when unavailable
     */
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

    public record ReportContext(
            String accessToken,
            String platform,
            String schoolCode) {
    }
}