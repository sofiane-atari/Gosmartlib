package edu.ap.bib_backend.mapper;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import org.springframework.stereotype.Component;

@Component
public class CampusMapper {

    public CampusDTO toDto(Campus campus) {
        if (campus == null) {
            return null;
        }

        Long schoolId = campus.getSchool() != null ? campus.getSchool().getId() : null;
        String schoolName = campus.getSchool() != null ? campus.getSchool().getName() : null;

        return new CampusDTO(
                campus.getId(),
                campus.getName(),
                campus.getCode(),
                schoolId,
                schoolName,
                campus.isDefaultCampus()
        );
    }
}