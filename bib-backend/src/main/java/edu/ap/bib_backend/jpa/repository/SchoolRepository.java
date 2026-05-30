package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByDomainName(String domainName);

    boolean existsByDomainName(String domainName);

    List<School> findByStatusAndMarkedForDeletionAtBefore(SchoolStatus status, LocalDateTime threshold);

    List<School> findByStatus(SchoolStatus status);

    List<School> findByIdIn(List<Long> ids);
}
