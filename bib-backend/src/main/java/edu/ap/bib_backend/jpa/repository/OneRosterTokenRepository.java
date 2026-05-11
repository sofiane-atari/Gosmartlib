package edu.ap.bib_backend.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.OneRosterToken;

public interface OneRosterTokenRepository extends JpaRepository<OneRosterToken, Long> {
    Optional<OneRosterToken> findBySchoolId(Long schoolId);
}
