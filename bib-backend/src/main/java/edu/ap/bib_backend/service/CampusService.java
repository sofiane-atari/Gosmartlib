package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.CampusMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CampusService {

    private final CampusRepository campusRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CampusMapper campusMapper;
    private final CurrentUserService currentUserService;

    public CampusService(
            CampusRepository campusRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            CampusMapper campusMapper,
            CurrentUserService currentUserService
    ) {
        this.campusRepository = campusRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.campusMapper = campusMapper;
        this.currentUserService = currentUserService;
    }

    public List<CampusDTO> getCampusesBySchool(Long schoolId) {
        return campusRepository.findBySchoolId(schoolId)
                .stream()
                .map(campusMapper::toDto)
                .toList();
    }

    public List<CampusDTO> getCampusesForCurrentUserSchool(Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);

        return campusRepository.findBySchoolId(school.getId())
                .stream()
                .map(campusMapper::toDto)
                .toList();
    }

    public CampusDTO getCampus(Long id, Authentication authentication) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Campus niet gevonden."));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (!isAdmin) {
            String userId = currentUserService.extractCurrentUserId(authentication);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden."));

            if (user.getSchool() == null || !Objects.equals(user.getSchool().getId(), campus.getSchool().getId())) {
                throw new AccessDeniedException("Geen toegang tot deze campus.");
            }
        }

        return campusMapper.toDto(campus);
    }

    @Transactional
    public CampusDTO createCampus(Long schoolId, CampusDTO dto) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "School niet gevonden."));

        String name = normalizeName(dto.getName());

        if (campusRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new ResourceAlreadyExistsException("name", name);
        }

        Campus campus = new Campus();
        campus.setSchool(school);
        campus.setName(name);
        updateCampusDetails(campus, dto);

        Campus saved = campusRepository.save(campus);
        log.info("campus '{}' created in school {} (id={})", saved.getName(), school.getId(), saved.getId());
        return campusMapper.toDto(saved);
    }

    @Transactional
    public CampusDTO updateCampus(Long id, CampusDTO dto) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Campus niet gevonden."));

        Long schoolId = campus.getSchool().getId();

        String name = normalizeName(dto.getName());

        campusRepository.findBySchoolId(schoolId).stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing -> existing.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("name", name);
                });

        campus.setName(name);
        updateCampusDetails(campus, dto);

        campus = campusRepository.save(campus);
        log.info("campus {} updated: name='{}'", id, campus.getName());
        return campusMapper.toDto(campus);
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campusnaam is verplicht.");
        }

        return value.trim();
    }

    private void updateCampusDetails(Campus campus, CampusDTO dto) {
        campus.setAddress(normalizeAddress(dto.getAddress()));
        campus.setPostalCode(dto.getPostalCode());
        campus.setContactPhone(trimOrNull(dto.getContactPhone()));
        campus.setContactEmail(trimOrNull(dto.getContactEmail()));
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
}
