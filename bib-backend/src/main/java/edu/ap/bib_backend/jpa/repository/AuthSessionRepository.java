package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
}