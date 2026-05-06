package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.UserBadge;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByProfileAndBadge(GamificationProfile profile, Badge badge);

    List<UserBadge> findByProfileOrderByEarnedAtDesc(GamificationProfile profile);

    List<UserBadge> findTop3ByProfileOrderByEarnedAtDesc(GamificationProfile profile);
}