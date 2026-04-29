package edu.ap.bib_backend.jpa.repository;

import java.util.Optional;

import edu.ap.bib_backend.jpa.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByDomainName(String domainName);
}