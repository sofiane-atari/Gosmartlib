package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public List<SchoolDTO> getAll() {
        return schoolRepository.findAll(Sort.by("name").ascending()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchoolDTO createSchool(SchoolDTO schoolDTO) {
        String normalizedDomainName = schoolDTO.domainName().trim().toLowerCase();
        String normalizedName = schoolDTO.name().trim();

        if (schoolRepository.existsByDomainNameIgnoreCase(normalizedDomainName)) {
            throw new ResourceAlreadyExistsException(
                    "domainName",
                    normalizedDomainName,
                    "School met domeinnaam '" + normalizedDomainName + "' bestaat al.");
        }

        if (schoolRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistsException(
                    "name",
                    normalizedName,
                    "School met naam '" + normalizedName + "' bestaat al.");
        }

        School school = new School();
        school.setDomainName(normalizedDomainName);
        school.setName(normalizedName);
        school.setAdres(schoolDTO.address().trim());
        school.setPostalCode(schoolDTO.postalCode());

        School savedSchool = schoolRepository.save(school);
        return toDto(savedSchool);
    }

    @Transactional
    public void deleteSchool(String domainName) {
        String normalizedDomainName = domainName.trim().toLowerCase();

        if (!schoolRepository.existsById(normalizedDomainName)) {
            throw new ResourceNotFoundException(
                    "domainName",
                    "School met domeinnaam '" + normalizedDomainName + "' niet gevonden.");
        }

        schoolRepository.deleteById(normalizedDomainName);
    }

    @Transactional
    public SchoolDTO updateSchool(String domainName, SchoolDTO schoolDTO) {
        String normalizedDomainName = domainName.trim().toLowerCase();
        String requestDomainName = schoolDTO.domainName().trim().toLowerCase();

        if (!requestDomainName.equals(normalizedDomainName)) {
            throw new IllegalArgumentException("Domeinnaam kan niet gewijzigd worden.");
        }

        School existingSchool = schoolRepository.findById(normalizedDomainName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "domainName",
                        "School met domeinnaam '" + normalizedDomainName + "' niet gevonden."));

        String normalizedName = schoolDTO.name().trim();
        if (schoolRepository.existsByNameIgnoreCaseAndDomainNameNot(normalizedName, normalizedDomainName)) {
            throw new ResourceAlreadyExistsException(
                    "name",
                    normalizedName,
                    "School met naam '" + normalizedName + "' bestaat al.");
        }

        existingSchool.setName(normalizedName);
        existingSchool.setAdres(schoolDTO.address().trim());
        existingSchool.setPostalCode(schoolDTO.postalCode());

        School updatedSchool = schoolRepository.save(existingSchool);
        return toDto(updatedSchool);
    }

    private SchoolDTO toDto(School school) {
        return new SchoolDTO(
                school.getDomainName(),
                school.getName(),
                school.getAdres(),
                school.getPostalCode());
    }
}
