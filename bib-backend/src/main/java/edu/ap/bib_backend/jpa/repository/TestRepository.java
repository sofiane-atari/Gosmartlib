package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    
}