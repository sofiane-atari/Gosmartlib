package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.dto.ReadingListShareDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final ReadingListBookRepository readingListBookRepository;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final CurrentUserService currentUserService;
    private final ActiveSchoolContext activeSchoolContext;

    private static final String FAVORITES_NAME = "Favorieten";
    private static final int MAX_CATALOG_FEATURED_LISTS = 2;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public ReadingListService(
            ReadingListRepository readingListRepository,
            ReadingListBookRepository readingListBookRepository,
            UserRepository userRepository,
            CampusRepository campusRepository,
            CurrentUserService currentUserService,
            ActiveSchoolContext activeSchoolContext) {
        this.readingListRepository = readingListRepository;
        this.readingListBookRepository = readingListBookRepository;
        this.userRepository = userRepository;
        this.campusRepository = campusRepository;
        this.currentUserService = currentUserService;
        this.activeSchoolContext = activeSchoolContext;
    }

    public ReadingList createReadingList(String userId, ReadingListDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) {
            throw new IllegalArgumentException("name mag niet null of leeg zijn");
        }

        User user = getUserOrThrow(userId);
        Campus campus = resolveReadingListCampus(user);
        AudienceContext audience = getAudienceContext();

        ReadingListTargetType targetType = dto.targetType() == null ? ReadingListTargetType.PERSONAL : dto.targetType();
        assertCanCreateList(targetType);
        String targetValue = normalizeTargetValue(targetType, dto.targetValue(), audience);

        ReadingList list = new ReadingList();
        list.setName(dto.name().trim());
        list.setUser(user);
        list.setCampus(campus);
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);

        return readingListRepository.save(list);
    }

    public List<ReadingList> getAllLists(String userId) {
        User user = getUserOrThrow(userId);
        Campus campus = resolveReadingListCampus(user);
        AudienceContext audience = getAudienceContext();
        ensureSharedListsExist(user, campus, audience);

        List<String> expandedClassCodes = expandClassCodePrefixes(audience.classCodes());

        return readingListRepository.findVisibleForUser(
                userId,
                campus.getId(),
                expandedClassCodes,
                audience.yearCode(),
                audience.schoolCode(),
                campus.getSchool().getId(),
                shouldIncludeOwnSharedLists());
    }

    public ReadingList getListById(String userId, Long id) {
        User user = getUserOrThrow(userId);
        Campus campus = resolveReadingListCampus(user);
        AudienceContext audience = getAudienceContext();

        List<String> expandedClassCodes = expandClassCodePrefixes(audience.classCodes());

        return readingListRepository.findVisibleById(
                id,
                userId,
                campus.getId(),
                expandedClassCodes,
                audience.yearCode(),
                audience.schoolCode(),
                campus.getSchool().getId(),
                shouldIncludeOwnSharedLists())
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst met id " + id + " bestaat niet"));
    }

    public void deleteReadingList(String userId, Long id) {
        ReadingList list = getListById(userId, id);

        if (isFavoritesList(list.getName())) {
            throw new IllegalArgumentException("Deze leeslijst kan niet verwijderd worden");
        }

        if (list.getTargetType() == ReadingListTargetType.SYSTEM) {
            if (!currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
                throw new IllegalArgumentException("Alleen bibliothecarissen en admins kunnen uitgelichte leeslijsten verwijderen");
            }

            readingListRepository.deleteById(id);
            return;
        }

        if (list.getTargetType() != ReadingListTargetType.PERSONAL
                && !currentUserService.currentUserHasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_LIBRARIAN")) {
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

    public ReadingList getOrCreateFavoritesList(String userId) {
        User user = getUserOrThrow(userId);
        Campus campus = resolveReadingListCampus(user);

        return readingListRepository
                .findByNameIgnoreCaseAndUser_IdAndTargetTypeAndCampusId(
                        FAVORITES_NAME,
                        userId,
                        ReadingListTargetType.PERSONAL,
                        campus.getId())
                .orElseGet(() -> {
                    ReadingList newFavorites = new ReadingList();
                    newFavorites.setName(FAVORITES_NAME);
                    newFavorites.setUser(user);
                    newFavorites.setCampus(campus);
                    newFavorites.setTargetType(ReadingListTargetType.PERSONAL);
                    newFavorites.setTargetValue(null);
                    return readingListRepository.save(newFavorites);
                });
    }

    public ReadingListBook addBookToFavorites(String userId, Long bookId) {
        ReadingList favoritesList = getOrCreateFavoritesList(userId);
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

    private Campus resolveReadingListCampus(User fallbackUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        School school = null;
        try {
            school = currentUserService.getCurrentUserSchool(authentication);
        } catch (Exception e) {
            // fall through to fallback
        }
        if (school == null || school.getId() == null) {
            school = fallbackUser.getSchool();
        }
        if (school == null || school.getId() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return campusRepository.findFirstBySchoolIdOrderByIdAsc(school.getId())
                .orElseThrow(() -> new IllegalStateException("School heeft geen campus voor leeslijsten."));
    }

    private School resolveSchool(User fallbackUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        School school = null;
        try {
            school = currentUserService.getCurrentUserSchool(authentication);
        } catch (Exception e) {
            // fall through to fallback
        }
        if (school == null || school.getId() == null) {
            school = fallbackUser.getSchool();
        }
        if (school == null || school.getId() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }
        return school;
    }

    private boolean isFavoritesList(String name) {
        return name != null && FAVORITES_NAME.equalsIgnoreCase(name.trim());
    }

    private String normalizeTargetValue(ReadingListTargetType targetType, String requestedValue,
            AudienceContext audience) {
        return switch (targetType) {
            case PERSONAL -> null;
            case CLASS ->
                (requestedValue == null || requestedValue.isBlank()) ? audience.primaryClassCode() : requestedValue.trim();
            case SYSTEM -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private AudienceContext getAudienceContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return AudienceContext.defaults();
        }

        List<String> classCodes = new ArrayList<>();
        String yearCode = AudienceContext.defaults().yearCode();
        String schoolCode = AudienceContext.defaults().schoolCode();

        if (activeSchoolContext.isClassOverrideActive()) {
            classCodes = new ArrayList<>(activeSchoolContext.getActiveClassCodes());
        }

        if (classCodes.isEmpty() && authentication.getDetails() instanceof Map<?, ?> detailsRaw) {
            Map<String, Object> details = (Map<String, Object>) detailsRaw;
            classCodes = readClassCodesFromDetails(details);
            yearCode = readDetail(details, "yearCode", AudienceContext.defaults().yearCode());
            schoolCode = readDetail(details, "schoolCode", AudienceContext.defaults().schoolCode());
        }

        Object principal = authentication.getPrincipal();
        if (classCodes.isEmpty() && principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            String singleClassCode = readPrincipalAttribute(oauthPrincipal, "classCode",
                    AudienceContext.defaults().primaryClassCode());
            classCodes = List.of(singleClassCode);
            yearCode = readPrincipalAttribute(oauthPrincipal, "yearCode", AudienceContext.defaults().yearCode());
            schoolCode = readPrincipalAttribute(oauthPrincipal, "schoolCode",
                    AudienceContext.defaults().schoolCode());
        }

        if (classCodes.isEmpty()) {
            classCodes = List.of(AudienceContext.defaults().primaryClassCode());
        }

        return new AudienceContext(classCodes, yearCode, schoolCode);
    }

    private List<String> readClassCodesFromDetails(Map<String, Object> details) {
        List<String> result = new ArrayList<>();
        Object raw = details.get("classCodes");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String str && !str.isBlank()) {
                    result.add(str.trim());
                }
            }
        }
        if (result.isEmpty()) {
            Object single = details.get("classCode");
            if (single instanceof String str && !str.isBlank()) {
                result.add(str.trim());
            }
        }
        return result;
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

    private void ensureSharedListsExist(User owner, Campus campus, AudienceContext audience) {
        if (!currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            List<String> classCodes = audience.classCodes();
            if (classCodes.isEmpty()) {
                classCodes = List.of(audience.primaryClassCode());
            }
            for (String classCode : classCodes) {
                ensureSharedList(owner,
                        campus,
                        ReadingListTargetType.CLASS,
                        classCode,
                        "Klas " + classCode);
            }
        }

        ensureSharedList(owner,
                campus,
                ReadingListTargetType.SYSTEM,
                null,
                "Uitgelicht");
    }

    private String extractSchoolName(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            return "School";
        }

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

        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private void ensureSharedList(
            User owner,
            Campus campus,
            ReadingListTargetType targetType,
            String targetValue,
            String defaultName) {
        if (targetType == ReadingListTargetType.SYSTEM) {
            if (readingListRepository.findFirstByTargetTypeAndCampus_School_Id(targetType, campus.getSchool().getId()).isPresent()) {
                return;
            }
        } else if (targetValue != null && readingListRepository
                .findFirstByTargetTypeAndTargetValueIgnoreCaseAndCampusId(targetType, targetValue, campus.getId())
                .isPresent()) {
            return;
        }

        ReadingList list = new ReadingList();
        list.setName(defaultName);
        list.setUser(owner);
        list.setCampus(campus);
        list.setTargetType(targetType);
        list.setTargetValue(targetValue);
        readingListRepository.save(list);
    }

    private void assertCanCreateList(ReadingListTargetType targetType) {
        if (targetType == ReadingListTargetType.PERSONAL) {
            return;
        }

        if (targetType == ReadingListTargetType.SYSTEM) {
            if (currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
                return;
            }

            throw new IllegalArgumentException("Alleen bibliothecarissen en admins kunnen uitgelichte leeslijsten aanmaken");
        }

        if (!currentUserService.currentUserHasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            throw new IllegalArgumentException("Alleen leerkrachten, bibliothecarissen en admins kunnen gedeelde leeslijsten maken");
        }
    }

    private void assertCanModifyList(ReadingList list) {
        if (list.getTargetType() == ReadingListTargetType.PERSONAL) {
            return;
        }

        if (list.getTargetType() == ReadingListTargetType.SYSTEM) {
            if (currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
                return;
            }

            throw new IllegalArgumentException("De systeembrede leeslijst kan niet aangepast worden");
        }

        if (!currentUserService.currentUserHasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen deze leeslijst aanpassen");
        }
    }

    private boolean shouldIncludeOwnSharedLists() {
        return currentUserService.currentUserHasAnyAuthority("ROLE_TEACHER");
    }

    private boolean isTeacherAuthenticated() {
        return currentUserService.currentUserHasAnyAuthority("ROLE_TEACHER");
    }

    private List<String> expandClassCodePrefixes(List<String> codes) {
        Set<String> expanded = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            String trimmed = code.trim().toLowerCase();
            expanded.add(trimmed);
            for (int i = trimmed.length() - 1; i > 0; i--) {
                String prefix = trimmed.substring(0, i);
                if (!prefix.isBlank()) {
                    expanded.add(prefix);
                }
            }
        }
        return new ArrayList<>(expanded);
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
                baseUrl + "/reading-list/shared/" + saved.getPublicShareId());
    }

    public ReadingList getSharedList(String publicShareId) {
        if (publicShareId == null || publicShareId.isBlank()) {
            throw new IllegalArgumentException("Ongeldige share id");
        }

        return readingListRepository.findByPublicShareIdAndShareEnabledTrue(publicShareId)
                .orElseThrow(() -> new IllegalArgumentException("Gedeelde leeslijst niet gevonden"));
    }

    public List<ReadingList> getCatalogFeaturedListsForCurrentUserSchool(String userId) {
        return getCatalogFeaturedLists(userId, null);
    }

    public List<ReadingList> getCatalogFeaturedLists(String userId, Long schoolId) {
        Long resolvedSchoolId = schoolId;

        if (resolvedSchoolId == null) {
            User user = getUserOrThrow(userId);
            resolvedSchoolId = user.getSchool().getId();
        }

        return readingListRepository
                .findTop2FeaturedBySchoolId(
                        ReadingListTargetType.SYSTEM,
                        resolvedSchoolId,
                        Pageable.ofSize(2));
    }

    public List<ReadingList> selectFeaturedListForCatalog(String userId, Long listId) {
        User user = getUserOrThrow(userId);
        School school = resolveSchool(user);

        ReadingList selectedList = readingListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst niet gevonden"));

        if (selectedList.getTargetType() != ReadingListTargetType.SYSTEM) {
            throw new IllegalArgumentException("Alleen uitgelichte leeslijsten kunnen op de catalogus getoond worden");
        }

        if (selectedList.getCampus() == null || !selectedList.getCampus().getSchool().getId().equals(school.getId())) {
            throw new IllegalArgumentException("Deze leeslijst hoort niet bij jouw school");
        }

        if (!currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            throw new IllegalArgumentException("Alleen beheerders kunnen uitgelichte leeslijsten beheren");
        }

        List<ReadingList> currentSelected = readingListRepository
                .findFeaturedBySchoolId(
                        ReadingListTargetType.SYSTEM,
                        school.getId());

        if (selectedList.isShowOnCatalog()) {
            return currentSelected;
        }

        if (currentSelected.size() >= MAX_CATALOG_FEATURED_LISTS) {
            ReadingList oldestSelected = currentSelected.get(0);
            oldestSelected.setShowOnCatalog(false);
            oldestSelected.setCatalogFeaturedOrder(null);
            readingListRepository.save(oldestSelected);
            currentSelected.remove(0);
        }

        int nextOrder = currentSelected.stream()
                .map(ReadingList::getCatalogFeaturedOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        selectedList.setShowOnCatalog(true);
        selectedList.setCatalogFeaturedOrder(nextOrder);
        readingListRepository.save(selectedList);

        return readingListRepository
                .findFeaturedBySchoolId(
                        ReadingListTargetType.SYSTEM,
                        school.getId());
    }

    public List<ReadingList> unselectFeaturedListForCatalog(String userId, Long listId) {
        User user = getUserOrThrow(userId);
        School school = resolveSchool(user);

        ReadingList list = readingListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst niet gevonden"));

        if (list.getTargetType() != ReadingListTargetType.SYSTEM) {
            throw new IllegalArgumentException("Alleen uitgelichte leeslijsten kunnen aangepast worden");
        }

        if (list.getCampus() == null || !list.getCampus().getSchool().getId().equals(school.getId())) {
            throw new IllegalArgumentException("Deze leeslijst hoort niet bij jouw school");
        }

        if (!currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            throw new IllegalArgumentException("Alleen beheerders kunnen uitgelichte leeslijsten beheren");
        }

        list.setShowOnCatalog(false);
        list.setCatalogFeaturedOrder(null);
        readingListRepository.save(list);

        return readingListRepository
                .findFeaturedBySchoolId(
                        ReadingListTargetType.SYSTEM,
                        school.getId());
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