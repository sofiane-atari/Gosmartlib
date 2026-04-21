package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingLevelRepository extends JpaRepository<ReadingLevel, Long> {
    ReadingLevel findByNiveauName(String niveauName);
}