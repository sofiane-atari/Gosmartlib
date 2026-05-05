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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

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
        String userId = currentUserService.extractCurrentUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden."));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return campusRepository.findBySchoolId(user.getSchool().getId())
                .stream()
                .map(campusMapper::toDto)
                .toList();
    }

    public CampusDTO getCampus(Long id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Campus niet gevonden."));

        return campusMapper.toDto(campus);
    }

    @Transactional
    public CampusDTO createCampus(Long schoolId, CampusDTO dto) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("id", "School niet gevonden."));

        String name = normalizeName(dto.getName());
        String code = dto.getCode() == null || dto.getCode().isBlank()
                ? slugify(name)
                : slugify(dto.getCode());

        if (campusRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new ResourceAlreadyExistsException("name", name);
        }

        if (campusRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
            throw new ResourceAlreadyExistsException("code", code);
        }

        boolean firstCampus = campusRepository.countBySchool(school) == 0;
        boolean shouldBeDefault = firstCampus || dto.isDefaultCampus();

        if (shouldBeDefault) {
            unsetDefaultCampusForSchool(schoolId);
        }

        Campus campus = new Campus();
        campus.setSchool(school);
        campus.setName(name);
        campus.setCode(code);
        campus.setDefaultCampus(shouldBeDefault);

        Campus saved = campusRepository.save(campus);
        return campusMapper.toDto(saved);
    }

    @Transactional
    public CampusDTO updateCampus(Long id, CampusDTO dto) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Campus niet gevonden."));

        Long schoolId = campus.getSchool().getId();

        String name = normalizeName(dto.getName());
        String code = dto.getCode() == null || dto.getCode().isBlank()
                ? slugify(name)
                : slugify(dto.getCode());

        campusRepository.findBySchoolId(schoolId).stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing -> existing.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("name", name);
                });

        campusRepository.findBySchoolId(schoolId).stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing -> existing.getCode().equalsIgnoreCase(code))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("code", code);
                });

        if (dto.isDefaultCampus()) {
            unsetDefaultCampusForSchool(schoolId);
            campus.setDefaultCampus(true);
        } else {
            boolean isOnlyDefaultCampus = campus.isDefaultCampus()
                    && campusRepository.findBySchoolId(schoolId).stream()
                    .filter(Campus::isDefaultCampus)
                    .count() == 1;

            if (isOnlyDefaultCampus) {
                throw new IllegalArgumentException("Elke school moet minstens één default-campus hebben.");
            }

            campus.setDefaultCampus(false);
        }

        campus.setName(name);
        campus.setCode(code);

        return campusMapper.toDto(campusRepository.save(campus));
    }

    private void unsetDefaultCampusForSchool(Long schoolId) {
        campusRepository.findBySchoolId(schoolId).forEach(campus -> {
            campus.setDefaultCampus(false);
            campusRepository.save(campus);
        });
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campusnaam is verplicht.");
        }

        return value.trim();
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campuscode is verplicht.");
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = normalized
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException("Campuscode is ongeldig.");
        }

        return slug;
    }
}