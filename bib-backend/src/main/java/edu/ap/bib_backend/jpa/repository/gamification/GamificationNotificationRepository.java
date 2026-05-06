package edu.ap.bib_backend.jpa.repository.gamification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationNotification;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;

public interface GamificationNotificationRepository extends JpaRepository<GamificationNotification, Long> {

    List<GamificationNotification> findTop5ByProfileAndReadAtIsNullOrderByCreatedAtDesc(
        GamificationProfile profile
);
}