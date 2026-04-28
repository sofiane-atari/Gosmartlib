package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public List<SchoolDTO> getAll() {
        return schoolRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchoolDTO create(SchoolDTO dto) {
        School school = new School();

        school.setDomainName(dto.domainName());
        school.setName(dto.name());
        school.setAddress(dto.address());
        school.setPostalCode(dto.postalCode());
        school.setContactPhone(dto.contactPhone());
        school.setContactEmail(dto.contactEmail());

        return toDto(schoolRepository.save(school));
    }

    @Transactional
    public void delete(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        schoolRepository.delete(school);
    }

    @Transactional
    public SchoolDTO update(Long id, SchoolDTO dto) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School niet gevonden."));

        school.setDomainName(dto.domainName());
        school.setName(dto.name());
        school.setAddress(dto.address());
        school.setPostalCode(dto.postalCode());
        school.setContactPhone(dto.contactPhone());
        school.setContactEmail(dto.contactEmail());

        return toDto(schoolRepository.save(school));
    }

    private SchoolDTO toDto(School school) {
        return new SchoolDTO(
                school.getId(),
                school.getDomainName(),
                school.getName(),
                school.getAddress(),
                school.getPostalCode(),
                school.getContactPhone(),
                school.getContactEmail(),
                school.getCreatedAt());
    }
}
