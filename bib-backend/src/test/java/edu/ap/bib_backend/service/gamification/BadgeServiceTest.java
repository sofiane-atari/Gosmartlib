package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.exception.gamification.BadgeNotFoundException;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.UserBadge;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeRepository;
import edu.ap.bib_backend.jpa.repository.gamification.UserBadgeRepository;
import edu.ap.bib_backend.service.gamification.util.GamificationSourceType;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    private static final String BADGE_CODE = "FIRST_REVIEW";
    private static final String BADGE_NAME = "Eerste recensie";
    private static final String BADGE_DESCRIPTION = "Je hebt je eerste recensie geschreven.";
    private static final String ASSET_KEY = "first-review";
    private static final String LOCKED_ASSET_KEY = "first-review-locked";
    private static final String THEME_COLOR = "#4F8EF7";
    private static final int TARGET_VALUE = 1;
    private static final int REWARD_POINTS = 2;
    private static final int DISPLAY_ORDER = 1;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PointService pointService;

    @Mock
    private GamificationNotificationService gamificationNotificationService;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    void givenActiveBadgeCode_whenFindActiveBadgeByCode_thenReturnsBadge() {
        // Arrange
        Badge badge = createActiveBadge();
        when(badgeRepository.findByCode(BADGE_CODE)).thenReturn(Optional.of(badge));

        // Act
        Badge result = badgeService.findActiveBadgeByCode(BADGE_CODE);

        // Assert
        assertThat(result).isSameAs(badge);
    }

    @Test
    void givenUnknownBadgeCode_whenFindActiveBadgeByCode_thenThrowsBadgeNotFoundException() {
        // Arrange
        when(badgeRepository.findByCode(BADGE_CODE)).thenReturn(Optional.empty());

        // Act
        BadgeNotFoundException exception = assertThrows(
                BadgeNotFoundException.class,
                () -> badgeService.findActiveBadgeByCode(BADGE_CODE)
        );

        // Assert
        assertThat(exception).isNotNull();
    }

    @Test
    void givenInactiveBadge_whenFindActiveBadgeByCode_thenThrowsBadgeNotFoundException() {
        // Arrange
        Badge badge = createActiveBadge();
        badge.setActive(false);

        when(badgeRepository.findByCode(BADGE_CODE)).thenReturn(Optional.of(badge));

        // Act
        BadgeNotFoundException exception = assertThrows(
                BadgeNotFoundException.class,
                () -> badgeService.findActiveBadgeByCode(BADGE_CODE)
        );

        // Assert
        assertThat(exception).isNotNull();
    }

    @Test
    void givenAlreadyEarnedBadge_whenAwardBadgeIfNotEarned_thenDoesNotAwardBadgeAgain() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createActiveBadge();

        when(userBadgeRepository.existsByProfileAndBadge(profile, badge)).thenReturn(true);

        // Act
        boolean awarded = badgeService.awardBadgeIfNotEarned(profile, badge);

        // Assert
        assertThat(awarded).isFalse();
        verify(userBadgeRepository, never()).save(org.mockito.ArgumentMatchers.any(UserBadge.class));
        verify(pointService, never()).awardPoints(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(gamificationNotificationService, never())
                .createBadgeUnlockedNotification(profile, badge);
    }

    @Test
    void givenNotEarnedBadge_whenAwardBadgeIfNotEarned_thenSavesBadgeAwardsPointsAndCreatesNotification() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createActiveBadge();

        when(userBadgeRepository.existsByProfileAndBadge(profile, badge)).thenReturn(false);

        // Act
        boolean awarded = badgeService.awardBadgeIfNotEarned(profile, badge);

        // Assert
        assertThat(awarded).isTrue();

        verify(userBadgeRepository).save(org.mockito.ArgumentMatchers.any(UserBadge.class));
        verify(pointService).awardPoints(
                profile,
                PointTransactionType.BADGE_REWARD,
                REWARD_POINTS,
                "Badge behaald: " + BADGE_NAME,
                GamificationSourceType.BADGE,
                BADGE_CODE
        );
        verify(gamificationNotificationService)
                .createBadgeUnlockedNotification(profile, badge);
    }

    private Badge createActiveBadge() {
        return new Badge(
                BADGE_CODE,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                BadgeCategory.REVIEWING,
                BadgeType.ONE_TIME,
                BadgeRarity.COMMON,
                TARGET_VALUE,
                REWARD_POINTS,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                THEME_COLOR,
                DISPLAY_ORDER
        );
    }
}