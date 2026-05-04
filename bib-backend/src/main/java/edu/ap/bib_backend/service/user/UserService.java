package edu.ap.bib_backend.service.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.ProfanityFilterService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;

    public UserService(UserRepository userRepository, ProfanityFilterService profanityFilterService,
            UserMapper userMapper, GroupService groupService, SchoolRepository schoolRepository,
            OneRosterClient oneRosterClient, TokenStorageService tokenStorageService) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public List<AuthUserDto> getUsers(UserGetOptions options) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = requireUserId(authentication);
        School school = getCurrentUserSchool(userId);

        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        return rosterUsers.stream()
                .map(this::toAuthUserDto)
                .filter(Objects::nonNull)
                .filter(dto -> matchesOptions(dto, options))
                .toList();
    }

    public User createUser(String userId, RoleEntity role, School school) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }

        User user = new User();
        user.setId(userId);

        user.setRole(Objects.requireNonNull(role, "role mag niet null zijn"));
        user.setSchool(Objects.requireNonNull(school, "school mag niet null zijn"));

        return userRepository.save(user);
    }

    public AuthUserDto setReviewBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + userId));

        user.setReviewBlocked(blocked);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        return authentication.getName();
    }

    private School getCurrentUserSchool(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Gebruiker niet gevonden: " + userId
                ));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return user.getSchool();
    }

    private AuthUserDto toAuthUserDto(Map<String, Object> rosterUser) {
        String id = readMetadataString(rosterUser, "smsc.legacyIdentifier");
        if (!StringUtils.hasText(id)) {
            return null;
        }

        String firstName = firstNonBlank(rosterUser, "givenName", "firstName");
        String lastName = firstNonBlank(rosterUser, "familyName", "lastName", "surname");
        String fullName = firstNonBlank(rosterUser, "fullName", "name", "displayName");
        if (!StringUtils.hasText(fullName)) {
            String combined = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName))
                    .trim();
            fullName = combined.isBlank() ? null : combined;
        }

        String email = firstNonBlank(rosterUser, "email", "emailAddress", "mail");

        return new AuthUserDto(
                id,
                firstName,
                lastName,
                fullName,
                null,
                email,
                null,
                null,
                null,
                List.of(),
                false);
    }

    private boolean matchesOptions(AuthUserDto dto, UserGetOptions options) {
        if (options == null) {
            return true;
        }

        if (!matchesFilter(dto.id(), options.getId())) {
            return false;
        }

        if (!matchesFilter(dto.firstName(), options.getFirstName())) {
            return false;
        }

        return matchesFilter(dto.lastName(), options.getLastName());
    }

    private boolean matchesFilter(String value, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return value.toLowerCase().contains(filter.trim().toLowerCase());
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }

        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String readMetadataString(Map<String, Object> user, String key) {
        if (user == null) {
            return null;
        }

        Object metadataObject = user.get("metadata");
        if (!(metadataObject instanceof Map<?, ?> metadata)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }

        return null;
    }
}
