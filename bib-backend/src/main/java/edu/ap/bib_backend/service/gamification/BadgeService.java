package edu.ap.bib_backend.service.gamification;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.exception.gamification.BadgeNotFoundException;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.UserBadge;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeRepository;
import edu.ap.bib_backend.jpa.repository.gamification.UserBadgeRepository;
import edu.ap.bib_backend.service.gamification.util.GamificationSourceType;

@Service
public class BadgeService {

    private static final String BADGE_REWARD_REASON_PREFIX = "Badge behaald: ";

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointService pointService;

    public BadgeService(
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            PointService pointService
    ) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.pointService = pointService;
    }

    @Transactional(readOnly = true)
    public Badge findActiveBadgeByCode(String badgeCode) {
        Badge badge = badgeRepository.findByCode(badgeCode)
                .orElseThrow(() -> new BadgeNotFoundException(badgeCode));

        if (!badge.isActive()) {
            throw new BadgeNotFoundException(badgeCode);
        }

        return badge;
    }

    @Transactional(readOnly = true)
    public List<Badge> findAllActiveBadges() {
        return badgeRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Badge> findActiveBadges(BadgeCategory category, BadgeRarity rarity) {
        if (category != null && rarity != null) {
            return badgeRepository.findByActiveTrueAndCategoryAndRarityOrderByDisplayOrderAsc(category, rarity);
        }

        if (category != null) {
            return badgeRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(category);
        }

        if (rarity != null) {
            return badgeRepository.findByActiveTrueAndRarityOrderByDisplayOrderAsc(rarity);
        }

        return findAllActiveBadges();
    }

    @Transactional(readOnly = true)
    public boolean hasEarnedBadge(GamificationProfile profile, Badge badge) {
        return userBadgeRepository.existsByProfileAndBadge(profile, badge);
    }

    @Transactional
    public boolean awardBadgeIfNotEarned(GamificationProfile profile, Badge badge) {
        if (hasEarnedBadge(profile, badge)) {
            return false;
        }

        UserBadge userBadge = new UserBadge(profile, badge);
        userBadgeRepository.save(userBadge);

        awardBadgeRewardPoints(profile, badge);

        return true;
    }

    private void awardBadgeRewardPoints(GamificationProfile profile, Badge badge) {
        if (badge.getRewardPoints() <= 0) {
            return;
        }

        pointService.awardPoints(
                profile,
                PointTransactionType.BADGE_REWARD,
                badge.getRewardPoints(),
                BADGE_REWARD_REASON_PREFIX + badge.getName(),
                GamificationSourceType.BADGE,
                badge.getCode()
        );
    }
}