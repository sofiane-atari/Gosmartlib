package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    private SchoolDTO toDto(School school) {
        return new SchoolDTO(
                school.getDomainName(),
                school.getName(),
                school.getAdres(),
                school.getPostalCode());
    }
}
