package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeProgressRepository extends JpaRepository<BadgeProgress, Long> {

    Optional<BadgeProgress> findByProfileAndBadge(GamificationProfile profile, Badge badge);

    List<BadgeProgress> findByProfile(GamificationProfile profile);

    @Query("""
            SELECT progress
            FROM BadgeProgress progress
            JOIN FETCH progress.badge badge
            WHERE progress.profile = :profile
              AND progress.completed = false
              AND progress.currentValue > 0
              AND badge.active = true
              AND badge.targetValue > 0
            ORDER BY (progress.currentValue * 1.0 / badge.targetValue) DESC,
                     progress.updatedAt DESC
            """)
    List<BadgeProgress> findAlmostEarnedBadgesByProfile(
            @Param("profile") GamificationProfile profile,
            Pageable pageable
    );
}