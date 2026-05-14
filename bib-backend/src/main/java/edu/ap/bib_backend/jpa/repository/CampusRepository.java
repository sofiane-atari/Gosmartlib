package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findBySchoolId(Long schoolId);

    Optional<Campus> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Campus> findFirstBySchoolIdOrderByIdAsc(Long schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    long countBySchool(School school);
}
