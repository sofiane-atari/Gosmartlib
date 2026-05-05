package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;

public interface BadgeProgressRepository extends JpaRepository<BadgeProgress, Long> {

    Optional<BadgeProgress> findByProfileAndBadge(GamificationProfile profile, Badge badge);

    List<BadgeProgress> findByProfile(GamificationProfile profile);
}