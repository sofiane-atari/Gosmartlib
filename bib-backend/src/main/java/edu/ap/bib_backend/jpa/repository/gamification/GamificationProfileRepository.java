package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;

public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, Long> {

    Optional<GamificationProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);
}