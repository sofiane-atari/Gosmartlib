package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import edu.ap.bib_backend.dto.ReadingListShareDTO;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ReadingListService {
    private final ReadingListRepository readingListRepository;
    private final ReadingListBookRepository readingListBookRepository;
    private final UserRepository userRepository;
    private static final String FAVORITES_NAME = "Favorieten";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public ReadingListService(ReadingListRepository readingListRepository,
            ReadingListBookRepository readingListBookRepository,
            UserRepository userRepository) {
        this.readingListRepository = readingListRepository;
        this.readingListBookRepository = readingListBookRepository;
        this.userRepository = userRepository;
    }

    public ReadingList createReadingList(String userId, ReadingListDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) {
            throw new IllegalArgumentException("name mag niet null of leeg zijn");
        }

        User user = getUserOrThrow(userId);
        AudienceContext audience = getAudienceContext();

        ReadingListTargetType targetType = dto.targetType() == null ? ReadingListTargetType.PERSONAL : dto.targetType();
        assertCanCreateList(targetType);
        String targetValue = normalizeTargetValue(targetType, dto.targetValue(), audience);

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());
        list.setUser(user);
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);

        return readingListRepository.save(list);
    }

    public List<ReadingList> getAllLists(String userId) {
        User user = getUserOrThrow(userId);
        AudienceContext audience = getAudienceContext();
        ensureSharedListsExist(user, audience);

        return readingListRepository.findVisibleForUser(userId,
                audience.classCode(),
                audience.yearCode(),
                audience.schoolCode(),
                shouldIncludeOwnSharedLists());
    }

    public ReadingList getListById(String userId, Long id) {
        getUserOrThrow(userId);
        AudienceContext audience = getAudienceContext();
        return readingListRepository.findVisibleById(id,
                        userId,
                        audience.classCode(),
                        audience.yearCode(),
                        audience.schoolCode(),
                        shouldIncludeOwnSharedLists())
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet"));
    }

    public void deleteReadingList(String userId, Long id) {
        ReadingList list = getListById(userId, id);

        if (isFavoritesList(list.getName()) || list.getTargetType() == ReadingListTargetType.SYSTEM) {
            throw new IllegalArgumentException("Deze leeslijst kan niet verwijderd worden");
        }

        if (list.getTargetType() != ReadingListTargetType.PERSONAL && !isTeacherAuthenticated()) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen deze leeslijst verwijderen");
        }

        readingListRepository.deleteById(id);
    }

    public ReadingListBook addBookToList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(userId, listId);
        assertCanModifyList(list);

        boolean exists = readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId);
        if (exists) {
            throw new IllegalArgumentException("Boek zit al in deze leeslijst");
        }

        ReadingListBook newBook = new ReadingListBook();
        newBook.setReadingList(list);
        newBook.setBookId(bookId);

        return readingListBookRepository.save(newBook);
    }

    public ReadingListBook addBookToFavorites(String userId, Long bookId) {
        User user = getUserOrThrow(userId);
        ReadingList favoritesList = readingListRepository
                .findByNameIgnoreCaseAndUser_IdAndTargetType(FAVORITES_NAME, userId, ReadingListTargetType.PERSONAL)
                .orElseGet(() -> {
                    ReadingList newFavorites = new ReadingList();
                    newFavorites.setName(FAVORITES_NAME);
                    newFavorites.setUser(user);
                    newFavorites.setTargetType(ReadingListTargetType.PERSONAL);
                    newFavorites.setTargetValue(null);
                    return readingListRepository.save(newFavorites);
                });

        return addBookToList(userId, favoritesList.getId(), bookId);
    }

    public void removeBookFromList(String userId, Long listId, Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Boek ID mag niet null zijn");
        }

        ReadingList list = getListById(userId, listId);
        assertCanModifyList(list);

        int deleted = readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId);
        if (deleted == 0) {
            throw new IllegalArgumentException("Boek met id " + bookId + " zit niet in deze leeslijst");
        }
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Gebruiker met id " + userId + " bestaat niet"));
    }

    private boolean isFavoritesList(String name) {
        return name != null && FAVORITES_NAME.equalsIgnoreCase(name.trim());
    }

    private String normalizeTargetValue(ReadingListTargetType targetType, String requestedValue, AudienceContext audience) {
        return switch (targetType) {
            case PERSONAL -> null;
            case CLASS -> (requestedValue == null || requestedValue.isBlank()) ? audience.classCode() : requestedValue.trim();
            case YEAR -> (requestedValue == null || requestedValue.isBlank()) ? audience.yearCode() : requestedValue.trim();
            case SCHOOL -> (requestedValue == null || requestedValue.isBlank()) ? audience.schoolCode() : requestedValue.trim();
            case SYSTEM -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private AudienceContext getAudienceContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return AudienceContext.defaults();
        }

        // JWT filter-pad: audience zit in authentication details map.
        if (authentication.getDetails() instanceof Map<?, ?> detailsRaw) {
            Map<String, Object> details = (Map<String, Object>) detailsRaw;
            String classCode = readDetail(details, "classCode", AudienceContext.defaults().classCode());
            String yearCode = readDetail(details, "yearCode", AudienceContext.defaults().yearCode());
            String schoolCode = readDetail(details, "schoolCode", AudienceContext.defaults().schoolCode());
            return new AudienceContext(classCode, yearCode, schoolCode);
        }

        // OAuth2 sessie-pad: audience zit in principal attributes.
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            String classCode = readPrincipalAttribute(oauthPrincipal, "classCode", AudienceContext.defaults().classCode());
            String yearCode = readPrincipalAttribute(oauthPrincipal, "yearCode", AudienceContext.defaults().yearCode());
            String schoolCode = readPrincipalAttribute(oauthPrincipal, "schoolCode", AudienceContext.defaults().schoolCode());
            return new AudienceContext(classCode, yearCode, schoolCode);
        }

        return AudienceContext.defaults();
    }

    private String readDetail(Map<String, Object> details, String key, String fallback) {
        Object value = details.get(key);
        if (value instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return fallback;
    }

    private String readPrincipalAttribute(OAuth2AuthenticatedPrincipal principal, String key, String fallback) {
        Object value = principal.getAttribute(key);
        if (value instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return fallback;
    }

    private void ensureSharedListsExist(User owner, AudienceContext audience) {
        ensureSharedList(owner,
                ReadingListTargetType.CLASS,
                audience.classCode(),
                "Klas " + audience.classCode());

        ensureSharedList(owner,
                ReadingListTargetType.YEAR,
                audience.yearCode(),
                "Jaar " + audience.yearCode());

        // School-specific list with real school name.
        String schoolName = extractSchoolName(audience.schoolCode());
        ensureSharedList(owner,
                ReadingListTargetType.SCHOOL,
                audience.schoolCode(),
                schoolName);

        // System-wide list visible to everyone (no targetValue).
        ensureSharedList(owner,
                ReadingListTargetType.SYSTEM,
                null,
                "Uitgelicht");
    }

    private String extractSchoolName(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            return "School";
        }
        // Extract domain or name from URL-like schoolCode.
        // Example: "https://aphogeschool.smartschool.be" -> "Aphogeschool"
        String normalized = schoolCode.trim();
        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            int slashIndex = normalized.indexOf('/', 8);
            if (slashIndex > 0) {
                normalized = normalized.substring(8, slashIndex);
            } else {
                normalized = normalized.substring(8);
            }
        }
        if (normalized.endsWith(".smartschool.be")) {
            normalized = normalized.substring(0, normalized.length() - ".smartschool.be".length());
        }
        // Capitalize first letter.
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private void ensureSharedList(User owner,
            ReadingListTargetType targetType,
            String targetValue,
            String defaultName) {
        if (targetType == ReadingListTargetType.SYSTEM) {
            if (readingListRepository.findFirstByTargetType(ReadingListTargetType.SYSTEM).isPresent()) {
                return;
            }
        } else if (targetValue != null && readingListRepository.findFirstByTargetTypeAndTargetValueIgnoreCase(targetType, targetValue).isPresent()) {
            return;
        }

        ReadingList list = new ReadingList();
        list.setName(defaultName);
        list.setUser(owner);
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);
        readingListRepository.save(list);
    }

    private void assertCanCreateList(ReadingListTargetType targetType) {
        if (targetType == ReadingListTargetType.PERSONAL) {
            return;
        }

        if (!isTeacherAuthenticated()) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen gedeelde leeslijsten maken");
        }
    }

    private void assertCanModifyList(ReadingList list) {
        if (list.getTargetType() == ReadingListTargetType.SYSTEM) {
            throw new IllegalArgumentException("De systeembrede leeslijst kan niet aangepast worden");
        }

        if (list.getTargetType() == ReadingListTargetType.PERSONAL) {
            return;
        }

        if (!isTeacherAuthenticated()) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen deze leeslijst aanpassen");
        }
    }

    private boolean shouldIncludeOwnSharedLists() {
        return isTeacherAuthenticated();
    }

    private boolean isTeacherAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_TEACHER"::equals);
    }

    public ReadingListShareDTO enableSharing(String userId, Long listId, String frontendBaseUrl) {
        ReadingList list = getListById(userId, listId);

        if (!list.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Je kan alleen je eigen leeslijsten delen");
        }

        if (list.getPublicShareId() == null || list.getPublicShareId().isBlank()) {
            list.setPublicShareId(generateUniquePublicShareId());
        }

        list.setShareEnabled(true);
        ReadingList saved = readingListRepository.save(list);

        String baseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:4200"
                : frontendBaseUrl.replaceAll("/+$", "");

        return new ReadingListShareDTO(
                saved.getPublicShareId(),
                baseUrl + "/reading-list/shared/" + saved.getPublicShareId()
        );
    }

    public ReadingList getSharedList(String publicShareId) {
        if (publicShareId == null || publicShareId.isBlank()) {
            throw new IllegalArgumentException("Ongeldige share id");
        }

        return readingListRepository.findByPublicShareIdAndShareEnabledTrue(publicShareId)
                .orElseThrow(() -> new IllegalArgumentException("Gedeelde leeslijst niet gevonden"));
    }

    private String generateUniquePublicShareId() {
        String id;

        do {
            id = "rl_" + randomUrlSafeToken(18);
        } while (readingListRepository.existsByPublicShareId(id));

        return id;
    }

    private String randomUrlSafeToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}


