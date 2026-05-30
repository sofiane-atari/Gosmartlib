package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final BookCopyRepository bookCopyRepository;
    private final OneRosterCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final OneRosterClient oneRosterClient;
    private final OneRosterUserSyncService oneRosterUserSyncService;
    private final CurrentUserService currentUserService;
    private final long gracePeriodDays;

    @PersistenceContext
    private EntityManager entityManager;

    public SchoolService(
            SchoolRepository schoolRepository,
            BookCopyRepository bookCopyRepository,
            OneRosterCredentialRepository credentialRepository,
            UserRepository userRepository,
            OneRosterClient oneRosterClient,
            OneRosterUserSyncService oneRosterUserSyncService,
            CurrentUserService currentUserService,
            @Value("${school.deletion.grace-period-days:30}") long gracePeriodDays
    ) {
        this.schoolRepository = schoolRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.oneRosterClient = oneRosterClient;
        this.oneRosterUserSyncService = oneRosterUserSyncService;
        this.currentUserService = currentUserService;
        this.gracePeriodDays = gracePeriodDays;
    }

    public List<SchoolDTO> getAll() {
        return schoolRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchoolDTO create(SchoolDTO dto) {
        OneRosterCredentialDTO credentialDto = dto.onerosterCredential();
        if (credentialDto == null) {
            throw new IllegalArgumentException("OneRoster credentials zijn verplicht bij het aanmaken van een school.");
        }

        String clientId = trimOrNull(credentialDto.clientId());
        String clientSecret = trimOrNull(credentialDto.clientSecret());

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("OneRoster client ID en client secret zijn verplicht.");
        }

        oneRosterClient.validateCredentials(clientId, clientSecret, dto.domainName());

        String normalized = normalizeDomainName(dto.domainName());
        if (schoolRepository.findByDomainName(normalized).isPresent()) {
            throw new IllegalArgumentException("School met domein '" + dto.domainName() + "' bestaat al.");
        }

        School school = new School();
        school.setDomainName(normalizeDomainName(dto.domainName()));
        school.setName(trimOrNull(dto.name()));
        school.setDefaultLoanDurationDays(dto.defaultLoanDurationDays() != null ? dto.defaultLoanDurationDays() : 15);
        school.setCampuses(createCampuses(dto.campuses(), school));

        School savedSchool = schoolRepository.save(school);

        OneRosterCredential credential = new OneRosterCredential();
        credential.setSchool(savedSchool);
        credential.setClientId(clientId);
        credential.setClientSecret(clientSecret);
        credentialRepository.save(credential);

        oneRosterUserSyncService.syncSchoolAsync(savedSchool);

        return toDto(savedSchool);
    }

    @Transactional
    public SchoolDTO update(Long id, SchoolDTO dto) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        String normalized = normalizeDomainName(dto.domainName());
        if (!school.getDomainName().equals(normalized) && schoolRepository.findByDomainName(normalized).isPresent()) {
            throw new IllegalArgumentException("School met domein '" + dto.domainName() + "' bestaat al.");
        }
        school.setDomainName(normalized);
        school.setName(trimOrNull(dto.name()));
        if (dto.defaultLoanDurationDays() != null) {
            school.setDefaultLoanDurationDays(dto.defaultLoanDurationDays());
        }

        if (dto.onerosterCredential() != null) {
            String clientId = trimOrNull(dto.onerosterCredential().clientId());
            String clientSecret = trimOrNull(dto.onerosterCredential().clientSecret());

            if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
                oneRosterClient.validateCredentials(clientId, clientSecret, dto.domainName());

                OneRosterCredential credential = credentialRepository.findBySchoolId(school.getId())
                        .orElseGet(OneRosterCredential::new);
                credential.setSchool(school);
                credential.setClientId(clientId);
                credential.setClientSecret(clientSecret);
                credentialRepository.save(credential);

                oneRosterUserSyncService.syncSchoolAsync(school);
            }
        }

        return toDto(schoolRepository.save(school));
    }

    @Transactional
    public void delete(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        school.setStatus(SchoolStatus.MARKED_FOR_DELETION);
        school.setMarkedForDeletionAt(LocalDateTime.now());
        schoolRepository.save(school);
    }

    @Transactional
    public SchoolDTO disable(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        school.setStatus(SchoolStatus.DISABLED);
        school.setMarkedForDeletionAt(null);
        return toDto(schoolRepository.save(school));
    }

    @Transactional
    public SchoolDTO enable(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        school.setStatus(SchoolStatus.ACTIVE);
        school.setMarkedForDeletionAt(null);
        return toDto(schoolRepository.save(school));
    }

    @Transactional
    public void purge(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new IllegalArgumentException("School niet gevonden.");
        }

        String schoolUsers =
                "SELECT u.id FROM users u WHERE u.school_id = :schoolId";
        String schoolCampuses =
                "SELECT c.id FROM campuses c WHERE c.school_id = :schoolId";
        String schoolBooks =
                "SELECT b.id FROM books b WHERE b.campus_id IN (" + schoolCampuses + ")";
        String schoolProfiles =
                "SELECT gp.id FROM gamification_profiles gp WHERE gp.user_id IN (" + schoolUsers + ")";
        String schoolLoans =
                "SELECT l.id FROM loans l WHERE l.campus_id IN (" + schoolCampuses + ")";
        String schoolReadingLists =
                "SELECT rl.id FROM reading_lists rl WHERE rl.user_id IN (" + schoolUsers
                        + ") OR rl.campus_id IN (" + schoolCampuses + ")";
        String schoolReviews =
                "SELECT r.id FROM reviews r WHERE r.user_id IN (" + schoolUsers
                        + ") OR r.book_id IN (" + schoolBooks + ")";

        executePurgeDelete("DELETE FROM gamification_notifications WHERE profile_id IN ("
                + schoolProfiles + ")", schoolId);
        executePurgeDelete("DELETE FROM gamification_badge_progress WHERE profile_id IN ("
                + schoolProfiles + ")", schoolId);
        executePurgeDelete("DELETE FROM gamification_user_badges WHERE profile_id IN ("
                + schoolProfiles + ")", schoolId);
        executePurgeDelete("DELETE FROM gamification_point_transactions WHERE profile_id IN ("
                + schoolProfiles + ")", schoolId);
        executePurgeDelete("DELETE FROM gamification_profiles WHERE user_id IN ("
                + schoolUsers + ")", schoolId);

        executePurgeDelete("DELETE FROM review_reports WHERE review_id IN ("
                + schoolReviews + ")", schoolId);
        executePurgeDelete("DELETE FROM review_reports WHERE reported_by_user_id IN ("
                + schoolUsers + ")", schoolId);
        executePurgeDelete("DELETE FROM reviews WHERE user_id IN (" + schoolUsers
                + ") OR book_id IN (" + schoolBooks + ")", schoolId);

        executePurgeDelete("DELETE FROM mail_subscriptions WHERE user_id IN ("
                + schoolUsers + ") OR book_id IN (" + schoolBooks + ")", schoolId);

        executePurgeDelete("DELETE FROM reading_list_books WHERE reading_list_id IN ("
                + schoolReadingLists + ")", schoolId);
        executePurgeDelete("DELETE FROM reading_lists WHERE user_id IN (" + schoolUsers
                + ") OR campus_id IN (" + schoolCampuses + ")", schoolId);

        executePurgeDelete("DELETE FROM lesson_tip_book_links WHERE book_id IN ("
                + schoolBooks + ")", schoolId);

        executePurgeDelete("DELETE FROM loan_copies WHERE loan_id IN ("
                + schoolLoans + ")", schoolId);
        executePurgeDelete("DELETE FROM loan_extensions WHERE loan_id IN ("
                + schoolLoans + ")", schoolId);
        executePurgeDelete("DELETE FROM loans WHERE campus_id IN ("
                + schoolCampuses + ")", schoolId);

        executePurgeDelete("DELETE FROM book_copies WHERE campus_id IN ("
                + schoolCampuses + ")", schoolId);
        executePurgeDelete("DELETE FROM book_genres WHERE book_id IN ("
                + schoolBooks + ")", schoolId);
        executePurgeDelete("DELETE FROM book_tags WHERE book_id IN ("
                + schoolBooks + ")", schoolId);
        executePurgeDelete("DELETE FROM books WHERE campus_id IN ("
                + schoolCampuses + ")", schoolId);

        executePurgeDelete("DELETE FROM auth_sessions WHERE user_id IN ("
                + schoolUsers + ")", schoolId);

        executePurgeDelete("DELETE FROM student_class_statistics WHERE student_sourced_id IN ("
                + "SELECT u.oneroster_sourced_id FROM users u WHERE u.school_id = :schoolId)",
                schoolId);

        executePurgeDelete("DELETE FROM user_groups WHERE user_id IN ("
                + schoolUsers + ")", schoolId);

        executePurgeDelete("DELETE FROM users WHERE school_id = :schoolId", schoolId);

        executePurgeDelete("DELETE FROM classes WHERE school_id = :schoolId", schoolId);
        executePurgeDelete("DELETE FROM tags WHERE school_id = :schoolId", schoolId);
        executePurgeDelete("DELETE FROM oneroster_tokens WHERE school_id = :schoolId", schoolId);
        executePurgeDelete("DELETE FROM oneroster_credentials WHERE school_id = :schoolId", schoolId);
        executePurgeDelete("DELETE FROM campuses WHERE school_id = :schoolId", schoolId);

        schoolRepository.deleteById(schoolId);
    }

    private void executePurgeDelete(String sql, Long schoolId) {
        entityManager.createNativeQuery(sql)
                .setParameter("schoolId", schoolId)
                .executeUpdate();
    }

    @Transactional
    public void setCatalogVisibility(Long schoolId, boolean visible, Authentication authentication) {
        School userSchool = currentUserService.getCurrentUserSchool(authentication);

        if (!userSchool.getId().equals(schoolId)) {
            throw new AccessDeniedException("Je kunt alleen de zichtbaarheid van je eigen school aanpassen.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        school.setCatalogVisible(visible);
        schoolRepository.save(school);
    }

    public void checkDomain(String domainName) {
        String normalized = normalizeDomainName(domainName);

        if (schoolRepository.existsByDomainName(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Er bestaat al een school met domein '" + domainName + "'."
            );
        }
    }

    public List<SchoolSummaryDto> getAccessibleSchools(Authentication authentication) {
        School userSchool = currentUserService.getCurrentUserSchool(authentication);
        Long userSchoolId = userSchool.getId();

        return schoolRepository.findByStatus(SchoolStatus.ACTIVE)
                .stream()
                .filter(school -> school.getCatalogVisible() || school.getId().equals(userSchoolId))
                .map(school -> new SchoolSummaryDto(
                        school.getId(),
                        school.getName(),
                        school.getDomainName(),
                        school.getDefaultLoanDurationDays(),
                        school.getStatus(),
                        school.getCatalogVisible()))
                .toList();
    }

    public School getSchoolById(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));
    }

    public long getBookCountBySchool(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new IllegalArgumentException("School niet gevonden.");
        }

        return bookCopyRepository.countByCampusSchoolId(schoolId);
    }

    public long getTotalBookCount() {
        return bookCopyRepository.count();
    }

    private SchoolDTO toDto(School school) {
        boolean hasLibrarian = !userRepository.findBySchoolIdAndRoleRole(school.getId(), Role.LIBRARIAN).isEmpty();

        Long daysUntilDeletion = null;
        if (school.getStatus() == SchoolStatus.MARKED_FOR_DELETION && school.getMarkedForDeletionAt() != null) {
            LocalDateTime deletionDate = school.getMarkedForDeletionAt().plusDays(gracePeriodDays);
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), deletionDate);
            daysUntilDeletion = Math.max(0, daysLeft);
        }

        OneRosterCredentialDTO credentialDto = null;

        return new SchoolDTO(
                school.getId(),
                school.getDomainName(),
                school.getName(),
                school.getDefaultLoanDurationDays(),
                school.getCampuses().stream()
                        .map(this::toCampusDto)
                        .toList(),
                school.getCreatedAt(),
                hasLibrarian,
                credentialDto,
                school.getStatus(),
                school.getMarkedForDeletionAt(),
                daysUntilDeletion,
                school.getCatalogVisible());
    }

    private List<Campus> createCampuses(List<CampusDTO> campusDtos, School school) {
        if (campusDtos == null || campusDtos.isEmpty()) {
            throw new IllegalArgumentException("Minstens één campus is verplicht bij het aanmaken van een school.");
        }

        boolean singleCampus = campusDtos.size() == 1;
        List<Campus> campuses = new ArrayList<>();
        Set<String> campusNames = new HashSet<>();

        for (CampusDTO campusDto : campusDtos) {
            String campusName = singleCampus ? school.getName() : normalizeCampusName(campusDto.getName());
            if (!campusNames.add(campusName.toLowerCase())) {
                throw new IllegalArgumentException("Campusnamen moeten uniek zijn binnen een school.");
            }

            Campus campus = new Campus();
            campus.setSchool(school);
            campus.setName(campusName);
            campus.setAddress(normalizeAddress(campusDto.getAddress()));
            campus.setPostalCode(campusDto.getPostalCode());
            campus.setContactPhone(trimOrNull(campusDto.getContactPhone()));
            campus.setContactEmail(trimOrNull(campusDto.getContactEmail()));
            campuses.add(campus);
        }

        return campuses;
    }

    private CampusDTO toCampusDto(Campus campus) {
        return new CampusDTO(
                campus.getId(),
                campus.getName(),
                campus.getSchool() != null ? campus.getSchool().getId() : null,
                campus.getSchool() != null ? campus.getSchool().getName() : null,
                campus.getAddress(),
                campus.getPostalCode(),
                campus.getContactPhone(),
                campus.getContactEmail()
        );
    }

    private String normalizeCampusName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campusnaam is verplicht wanneer een school meerdere campussen heeft.");
        }

        return value.trim();
    }

    private String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Adres is verplicht.");
        }

        return value.trim();
    }

    private String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeDomainName(String domainName) {
        return domainName == null ? null : domainName.trim().toLowerCase();
    }
}
