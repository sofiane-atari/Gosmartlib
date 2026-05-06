package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeProgressRepository;

@ExtendWith(MockitoExtension.class)
class BadgeProgressServiceTest {

    private static final String BADGE_CODE = "BOOKWORM_5_BOOKS";
    private static final String BADGE_NAME = "Boekenwurm";
    private static final String BADGE_DESCRIPTION = "Je hebt vijf boeken teruggebracht.";
    private static final String ASSET_KEY = "bookworm-5";
    private static final String LOCKED_ASSET_KEY = "bookworm-5-locked";
    private static final String THEME_COLOR = "#F28C28";
    private static final int TARGET_VALUE = 5;
    private static final int REWARD_POINTS = 5;
    private static final int DISPLAY_ORDER = 2;

    @Mock
    private BadgeProgressRepository badgeProgressRepository;

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private BadgeProgressService badgeProgressService;


    @Test
    void givenExistingProgress_whenIncreaseProgress_thenUpdatesCurrentValue() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(profile, badge);
        progress.setCurrentValue(2);

        when(badgeService.findActiveBadgeByCode(BADGE_CODE)).thenReturn(badge);
        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.of(progress));
        when(badgeProgressRepository.save(progress)).thenReturn(progress);

        // Act
        BadgeProgress result = badgeProgressService.increaseProgress(
                profile,
                BADGE_CODE,
                1
        );

        // Assert
        assertThat(result.getCurrentValue()).isEqualTo(3);
        assertThat(result.isCompleted()).isFalse();

        verify(badgeProgressRepository).save(progress);
        verify(badgeService, never()).awardBadgeIfNotEarned(profile, badge);
    }

    @Test
    void givenProgressReachesTarget_whenIncreaseProgress_thenCompletesProgressAndAwardsBadge() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(profile, badge);
        progress.setCurrentValue(4);

        when(badgeService.findActiveBadgeByCode(BADGE_CODE)).thenReturn(badge);
        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.of(progress));
        when(badgeProgressRepository.save(progress)).thenReturn(progress);

        // Act
        BadgeProgress result = badgeProgressService.increaseProgress(
                profile,
                BADGE_CODE,
                1
        );

        // Assert
        assertThat(result.getCurrentValue()).isEqualTo(TARGET_VALUE);
        assertThat(result.isCompleted()).isTrue();

        verify(badgeService).awardBadgeIfNotEarned(profile, badge);
        verify(badgeProgressRepository).save(progress);
    }

    @Test
    void givenProgressExceedsTarget_whenIncreaseProgress_thenLimitsCurrentValueToTarget() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(profile, badge);
        progress.setCurrentValue(4);

        when(badgeService.findActiveBadgeByCode(BADGE_CODE)).thenReturn(badge);
        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.of(progress));
        when(badgeProgressRepository.save(progress)).thenReturn(progress);

        // Act
        BadgeProgress result = badgeProgressService.increaseProgress(
                profile,
                BADGE_CODE,
                10
        );

        // Assert
        assertThat(result.getCurrentValue()).isEqualTo(TARGET_VALUE);
        assertThat(result.isCompleted()).isTrue();

        verify(badgeService).awardBadgeIfNotEarned(profile, badge);
    }

    @Test
    void givenCompletedProgress_whenIncreaseProgress_thenDoesNotUpdateOrAwardAgain() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(profile, badge);
        progress.setCurrentValue(TARGET_VALUE);
        progress.setCompleted(true);

        when(badgeService.findActiveBadgeByCode(BADGE_CODE)).thenReturn(badge);
        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.of(progress));

        // Act
        BadgeProgress result = badgeProgressService.increaseProgress(
                profile,
                BADGE_CODE,
                1
        );

        // Assert
        assertThat(result).isSameAs(progress);
        assertThat(result.getCurrentValue()).isEqualTo(TARGET_VALUE);
        assertThat(result.isCompleted()).isTrue();

        verify(badgeProgressRepository, never()).save(progress);
        verify(badgeService, never()).awardBadgeIfNotEarned(profile, badge);
    }

    @Test
    void givenExistingProgress_whenFindProgress_thenReturnsExistingProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();
        BadgeProgress progress = new BadgeProgress(profile, badge);

        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.of(progress));

        // Act
        BadgeProgress result = badgeProgressService.findProgress(profile, badge);

        // Assert
        assertThat(result).isSameAs(progress);
    }

    @Test
    void givenNoExistingProgress_whenFindProgress_thenReturnsUnsavedProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        Badge badge = createBadge();

        when(badgeProgressRepository.findByProfileAndBadge(profile, badge))
                .thenReturn(Optional.empty());

        // Act
        BadgeProgress result = badgeProgressService.findProgress(profile, badge);

        // Assert
        assertThat(result.getProfile()).isSameAs(profile);
        assertThat(result.getBadge()).isSameAs(badge);
        assertThat(result.getCurrentValue()).isEqualTo(0);
        assertThat(result.isCompleted()).isFalse();

        verify(badgeProgressRepository, never()).save(
                org.mockito.ArgumentMatchers.any(BadgeProgress.class)
        );
    }

    private Badge createBadge() {
        return new Badge(
                BADGE_CODE,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                BadgeCategory.READING,
                BadgeType.COUNT_BASED,
                BadgeRarity.RARE,
                TARGET_VALUE,
                REWARD_POINTS,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                THEME_COLOR,
                DISPLAY_ORDER
        );
    }
}