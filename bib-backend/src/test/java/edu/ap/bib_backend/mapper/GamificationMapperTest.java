package edu.ap.bib_backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.dto.gamification.GamificationBadgeDTO;
import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationNotification;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.enums.gamification.GamificationNotificationType;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;

class GamificationMapperTest {

    private static final String BADGE_CODE = "REVIEWER_5_REVIEWS";
    private static final String BADGE_NAME = "Recensie-expert";
    private static final String BADGE_DESCRIPTION = "Je hebt vijf recensies geschreven.";
    private static final String ASSET_KEY = "reviewer-5-reviews";
    private static final String LOCKED_ASSET_KEY = "reviewer-5-reviews-locked";
    private static final String THEME_COLOR = "#7C3AED";
    private static final int TARGET_VALUE = 5;
    private static final int REWARD_POINTS = 10;
    private static final int DISPLAY_ORDER = 5;

    private final GamificationMapper gamificationMapper = new GamificationMapper();


    @Test
    void givenBadge_whenToLockedBadgeDTO_thenMapsBadgeAsNotEarned() {
        // Arrange
        Badge badge = createBadge();

        // Act
        GamificationBadgeDTO result = gamificationMapper.toLockedBadgeDTO(badge);

        // Assert
        assertThat(result.code()).isEqualTo(BADGE_CODE);
        assertThat(result.earned()).isFalse();
        assertThat(result.earnedAt()).isNull();
    }

    @Test
    void givenPartialProgress_whenToProgressDTO_thenCalculatesProgressPercentage() {
        // Arrange
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(new GamificationProfile(), badge);
        progress.setCurrentValue(2);

        // Act
        GamificationProgressDTO result = gamificationMapper.toProgressDTO(progress);

        // Assert
        assertThat(result.badgeCode()).isEqualTo(BADGE_CODE);
        assertThat(result.currentValue()).isEqualTo(2);
        assertThat(result.targetValue()).isEqualTo(TARGET_VALUE);
        assertThat(result.progressPercentage()).isEqualTo(40);
        assertThat(result.completed()).isFalse();
    }

    @Test
    void givenProgressAboveTarget_whenToProgressDTO_thenLimitsPercentageToHundred() {
        // Arrange
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(new GamificationProfile(), badge);
        progress.setCurrentValue(8);

        // Act
        GamificationProgressDTO result = gamificationMapper.toProgressDTO(progress);

        // Assert
        assertThat(result.progressPercentage()).isEqualTo(100);
    }


    @Test
    void givenNotification_whenToNotificationDTO_thenMapsNotificationData() {
        // Arrange
        GamificationNotification notification = new GamificationNotification(
                new GamificationProfile(),
                GamificationNotificationType.BADGE_UNLOCKED,
                "Badge behaald",
                "Je hebt de badge 'Recensie-expert' ontgrendeld.",
                ASSET_KEY,
                THEME_COLOR,
                BadgeRarity.EPIC,
                REWARD_POINTS
        );
        notification.setId(1L);
        LocalDateTime createdAt = notification.getCreatedAt();

        // Act
        GamificationNotificationDTO result = gamificationMapper.toNotificationDTO(notification);

        // Assert
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo("BADGE_UNLOCKED");
        assertThat(result.title()).isEqualTo("Badge behaald");
        assertThat(result.assetKey()).isEqualTo(ASSET_KEY);
        assertThat(result.themeColor()).isEqualTo(THEME_COLOR);
        assertThat(result.rarity()).isEqualTo("EPIC");
        assertThat(result.rewardPoints()).isEqualTo(REWARD_POINTS);
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    private Badge createBadge() {
        return new Badge(
                BADGE_CODE,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                BadgeCategory.REVIEWING,
                BadgeType.COUNT_BASED,
                BadgeRarity.EPIC,
                TARGET_VALUE,
                REWARD_POINTS,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                THEME_COLOR,
                DISPLAY_ORDER
        );
    }
}