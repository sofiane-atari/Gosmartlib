package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final BookCopyRepository bookCopyRepository;
    private final OneRosterCredentialRepository credentialRepository;
    private final OneRosterClient oneRosterClient;
    private final OneRosterUserSyncService oneRosterUserSyncService;

    public SchoolService(
            SchoolRepository schoolRepository,
            BookCopyRepository bookCopyRepository,
            OneRosterCredentialRepository credentialRepository,
            OneRosterClient oneRosterClient,
            OneRosterUserSyncService oneRosterUserSyncService
    ) {
        this.schoolRepository = schoolRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.credentialRepository = credentialRepository;
        this.oneRosterClient = oneRosterClient;
        this.oneRosterUserSyncService = oneRosterUserSyncService;
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

    public void delete(Long id) {
        if (!schoolRepository.existsById(id)) {
            throw new IllegalArgumentException("School niet gevonden.");
        }

        schoolRepository.deleteById(id);
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
        return new SchoolDTO(
                school.getId(),
                school.getDomainName(),
                school.getName(),
                school.getDefaultLoanDurationDays(),
                school.getCampuses().stream()
                        .map(this::toCampusDto)
                        .toList(),
                school.getCreatedAt(),
                null);
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
