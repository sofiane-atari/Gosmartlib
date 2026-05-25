package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.School;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByDomainName(String domainName);

    boolean existsByDomainName(String domainName);
}
