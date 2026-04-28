package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final BookCopyRepository bookCopyRepository;

    public SchoolService(
            SchoolRepository schoolRepository,
            BookCopyRepository bookCopyRepository) {
        this.schoolRepository = schoolRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    public List<SchoolDTO> getAll() {
        return schoolRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

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

        return bookCopyRepository.countBySchoolId(schoolId);
    }

    public long getTotalBookCount() {
        return bookCopyRepository.count();
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