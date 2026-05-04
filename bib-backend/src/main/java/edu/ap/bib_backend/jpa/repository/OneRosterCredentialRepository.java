package edu.ap.bib_backend.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.OneRosterCredential;

public interface OneRosterCredentialRepository extends JpaRepository<OneRosterCredential, Long> {
    Optional<OneRosterCredential> findBySchoolId(Long schoolId);
}
