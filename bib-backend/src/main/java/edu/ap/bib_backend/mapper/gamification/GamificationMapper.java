package edu.ap.bib_backend.mapper.gamification;

import org.springframework.stereotype.Component;

import edu.ap.bib_backend.dto.gamification.GamificationBadgeDTO;
import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.dto.gamification.GamificationRecentEventDTO;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationNotification;
import edu.ap.bib_backend.jpa.entity.gamification.PointTransaction;
import edu.ap.bib_backend.jpa.entity.gamification.UserBadge;

@Component
public class GamificationMapper {

    private static final int FULL_PROGRESS_PERCENTAGE = 100;
    private static final int EMPTY_PROGRESS_PERCENTAGE = 0;

    public GamificationBadgeDTO toEarnedBadgeDTO(UserBadge userBadge) {
        Badge badge = userBadge.getBadge();

        return new GamificationBadgeDTO(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getAssetKey(),
                badge.getLockedAssetKey(),
                badge.getRarity().name(),
                badge.getThemeColor(),
                badge.getCategory().name(),
                badge.getRewardPoints(),
                true,
                userBadge.getEarnedAt()
        );
    }

    public GamificationBadgeDTO toLockedBadgeDTO(Badge badge) {
        return new GamificationBadgeDTO(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getAssetKey(),
                badge.getLockedAssetKey(),
                badge.getRarity().name(),
                badge.getThemeColor(),
                badge.getCategory().name(),
                badge.getRewardPoints(),
                false,
                null
        );
    }

    public GamificationProgressDTO toProgressDTO(BadgeProgress progress) {
        Badge badge = progress.getBadge();

        return new GamificationProgressDTO(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getAssetKey(),
                badge.getLockedAssetKey(),
                badge.getRarity().name(),
                badge.getThemeColor(),
                badge.getCategory().name(),
                progress.getCurrentValue(),
                badge.getTargetValue(),
                calculateProgressPercentage(progress.getCurrentValue(), badge.getTargetValue()),
                progress.isCompleted()
        );
    }

    public GamificationRecentEventDTO toRecentEventDTO(PointTransaction transaction) {
        return new GamificationRecentEventDTO(
                transaction.getType().name(),
                transaction.getPoints(),
                transaction.getReason(),
                transaction.getCreatedAt()
        );
    }

    public GamificationNotificationDTO toNotificationDTO(
            GamificationNotification notification
    ) {
        return new GamificationNotificationDTO(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getAssetKey(),
                notification.getThemeColor(),
                notification.getRarity().name(),
                notification.getRewardPoints(),
                notification.getCreatedAt()
        );
    }

    private int calculateProgressPercentage(int currentValue, int targetValue) {
        if (targetValue <= 0) {
            return EMPTY_PROGRESS_PERCENTAGE;
        }

        int percentage = currentValue * FULL_PROGRESS_PERCENTAGE / targetValue;

        return Math.min(percentage, FULL_PROGRESS_PERCENTAGE);
    }
}