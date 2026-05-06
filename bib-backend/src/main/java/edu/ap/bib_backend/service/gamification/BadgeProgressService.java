package edu.ap.bib_backend.service.gamification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeProgressRepository;

@Service
public class BadgeProgressService {

    private final BadgeProgressRepository badgeProgressRepository;
    private final BadgeService badgeService;

    public BadgeProgressService(
            BadgeProgressRepository badgeProgressRepository,
            BadgeService badgeService
    ) {
        this.badgeProgressRepository = badgeProgressRepository;
        this.badgeService = badgeService;
    }

    @Transactional
    public BadgeProgress increaseProgress(
            GamificationProfile profile,
            String badgeCode,
            int amount
    ) {
        Badge badge = badgeService.findActiveBadgeByCode(badgeCode);
        BadgeProgress progress = getOrCreateProgress(profile, badge);

        if (progress.isCompleted()) {
            return progress;
        }

        int updatedValue = progress.getCurrentValue() + amount;
        progress.setCurrentValue(limitToTargetValue(updatedValue, badge.getTargetValue()));

        if (hasReachedTarget(progress, badge)) {
            progress.setCompleted(true);
            badgeService.awardBadgeIfNotEarned(profile, badge);
        }

        return badgeProgressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public BadgeProgress findProgress(GamificationProfile profile, Badge badge) {
        return badgeProgressRepository.findByProfileAndBadge(profile, badge)
                .orElseGet(() -> new BadgeProgress(profile, badge));
    }

    private BadgeProgress getOrCreateProgress(GamificationProfile profile, Badge badge) {
        return badgeProgressRepository.findByProfileAndBadge(profile, badge)
                .orElseGet(() -> badgeProgressRepository.save(new BadgeProgress(profile, badge)));
    }

    private boolean hasReachedTarget(BadgeProgress progress, Badge badge) {
        return progress.getCurrentValue() >= badge.getTargetValue();
    }

    private int limitToTargetValue(int currentValue, int targetValue) {
        return Math.min(currentValue, targetValue);
    }
}