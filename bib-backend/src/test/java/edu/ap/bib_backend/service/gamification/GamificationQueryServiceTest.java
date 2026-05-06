package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.gamification.GamificationBadgeDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.dto.gamification.GamificationRecentEventDTO;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.PointTransaction;
import edu.ap.bib_backend.jpa.entity.gamification.UserBadge;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeProgressRepository;
import edu.ap.bib_backend.jpa.repository.gamification.PointTransactionRepository;
import edu.ap.bib_backend.jpa.repository.gamification.UserBadgeRepository;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class GamificationQueryServiceTest {

    private static final String USER_ID = "student-user-id";

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
    private UserRepository userRepository;

    @Mock
    private GamificationProfileService gamificationProfileService;

    @Mock
    private BadgeService badgeService;

    @Mock
    private BadgeProgressService badgeProgressService;

    @Mock
    private BadgeProgressRepository badgeProgressRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private GamificationMapper gamificationMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GamificationQueryService gamificationQueryService;

    @Test
    void givenAuthenticatedUser_whenGetCurrentUserProfile_thenReturnsProfileForCurrentUser() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = createProfile(user);

        UserBadge latestUserBadge = mock(UserBadge.class);
        UserBadge earnedUserBadge = mock(UserBadge.class);
        PointTransaction transaction = mock(PointTransaction.class);

        Badge badge = createBadge();
        BadgeProgress progress = createProgress(profile, badge, 2, false);

        GamificationBadgeDTO latestAchievementDto = createBadgeDto(true);
        GamificationBadgeDTO earnedBadgeDto = createBadgeDto(true);
        GamificationProgressDTO progressDto = createProgressDto(false);
        GamificationRecentEventDTO recentEventDto = createRecentEventDto();

        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);

        when(userBadgeRepository.findTop3ByProfileOrderByEarnedAtDesc(profile))
                .thenReturn(List.of(latestUserBadge));
        when(userBadgeRepository.findByProfileOrderByEarnedAtDesc(profile))
                .thenReturn(List.of(earnedUserBadge));
        when(badgeService.findAllActiveBadges()).thenReturn(List.of(badge));
        when(badgeProgressService.findProgress(profile, badge)).thenReturn(progress);
        when(pointTransactionRepository.findTop10ByProfileOrderByCreatedAtDesc(profile))
                .thenReturn(List.of(transaction));

        when(gamificationMapper.toEarnedBadgeDTO(latestUserBadge)).thenReturn(latestAchievementDto);
        when(gamificationMapper.toEarnedBadgeDTO(earnedUserBadge)).thenReturn(earnedBadgeDto);
        when(gamificationMapper.toProgressDTO(progress)).thenReturn(progressDto);
        when(gamificationMapper.toRecentEventDTO(transaction)).thenReturn(recentEventDto);

        // Act
        GamificationProfileDTO result =
                gamificationQueryService.getCurrentUserProfile(authentication);

        // Assert
        assertThat(result.totalPoints()).isEqualTo(profile.getTotalPoints());
        assertThat(result.currentLevel()).isEqualTo(profile.getCurrentLevel());
        assertThat(result.pointsUntilNextLevel()).isEqualTo(profile.getPointsUntilNextLevel());
        assertThat(result.latestAchievements()).containsExactly(latestAchievementDto);
        assertThat(result.earnedBadges()).containsExactly(earnedBadgeDto);
        assertThat(result.badgeProgress()).containsExactly(progressDto);
        assertThat(result.recentEvents()).containsExactly(recentEventDto);
    }

    @Test
    void givenBadgeFiltersAndCompletedFalse_whenGetBadgesForUserId_thenReturnsOnlyIncompleteBadges() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = createProfile(user);

        Badge completedBadge = createBadge("FIRST_BOOK_RETURNED", BadgeCategory.READING, BadgeRarity.COMMON);
        Badge incompleteBadge = createBadge("BOOKWORM_5_BOOKS", BadgeCategory.READING, BadgeRarity.RARE);

        BadgeProgress completedProgress = createProgress(profile, completedBadge, 1, true);
        BadgeProgress incompleteProgress = createProgress(profile, incompleteBadge, 2, false);

        GamificationProgressDTO incompleteProgressDto = createProgressDto(false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);

        when(badgeService.findActiveBadges(BadgeCategory.READING, BadgeRarity.RARE))
                .thenReturn(List.of(completedBadge, incompleteBadge));
        when(badgeProgressService.findProgress(profile, completedBadge)).thenReturn(completedProgress);
        when(badgeProgressService.findProgress(profile, incompleteBadge)).thenReturn(incompleteProgress);
        when(gamificationMapper.toProgressDTO(incompleteProgress)).thenReturn(incompleteProgressDto);

        // Act
        List<GamificationProgressDTO> result = gamificationQueryService.getBadgesForUserId(
                USER_ID,
                BadgeCategory.READING,
                BadgeRarity.RARE,
                false
        );

        // Assert
        assertThat(result).containsExactly(incompleteProgressDto);
        verify(gamificationMapper).toProgressDTO(incompleteProgress);
    }

    @Test
    void givenAlmostEarnedBadges_whenGetAlmostEarnedBadgesForUserId_thenReturnsMappedDtosWithLimitThree() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = createProfile(user);

        Badge badge = createBadge();
        BadgeProgress progress = createProgress(profile, badge, 4, false);
        GamificationProgressDTO progressDto = createProgressDto(false);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);
        when(badgeProgressRepository.findAlmostEarnedBadgesByProfile(
                org.mockito.ArgumentMatchers.eq(profile),
                pageableCaptor.capture()
        )).thenReturn(List.of(progress));
        when(gamificationMapper.toProgressDTO(progress)).thenReturn(progressDto);

        // Act
        List<GamificationProgressDTO> result =
                gamificationQueryService.getAlmostEarnedBadgesForUserId(USER_ID);

        // Assert
        assertThat(result).containsExactly(progressDto);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void givenUnsafePagingOptions_whenGetRecentEventsForUserId_thenUsesSafePageAndMaxSize() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = createProfile(user);

        PointTransaction transaction = mock(PointTransaction.class);
        GamificationRecentEventDTO recentEventDto = createRecentEventDto();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);
        when(pointTransactionRepository.findByProfileOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(profile),
                pageableCaptor.capture()
        )).thenReturn(new PageImpl<>(List.of(transaction)));
        when(gamificationMapper.toRecentEventDTO(transaction)).thenReturn(recentEventDto);

        // Act
        PageDTO<GamificationRecentEventDTO> result =
                gamificationQueryService.getRecentEventsForUserId(USER_ID, -5, 999);

        // Assert
        assertThat(result.data()).containsExactly(recentEventDto);
        assertThat(result.meta().currentPage()).isEqualTo(0);
        assertThat(result.meta().pageSize()).isEqualTo(1);

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void givenAuthenticatedUser_whenGetCurrentUserRecentEvents_thenDelegatesUsingCurrentUserId() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = createProfile(user);

        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);
        when(pointTransactionRepository.findByProfileOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        PageDTO<GamificationRecentEventDTO> result =
                gamificationQueryService.getCurrentUserRecentEvents(authentication, 0, 5);

        // Assert
        assertThat(result.data()).isEmpty();
        verify(currentUserService).extractCurrentUserId(authentication);
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    private GamificationProfile createProfile(User user) {
        GamificationProfile profile = new GamificationProfile(user);
        profile.setTotalPoints(25);
        profile.setCurrentLevel(1);
        profile.setPointsUntilNextLevel(75);
        return profile;
    }

    private Badge createBadge() {
        return createBadge(BADGE_CODE, BadgeCategory.READING, BadgeRarity.RARE);
    }

    private Badge createBadge(
            String code,
            BadgeCategory category,
            BadgeRarity rarity
    ) {
        return new Badge(
                code,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                category,
                BadgeType.COUNT_BASED,
                rarity,
                TARGET_VALUE,
                REWARD_POINTS,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                THEME_COLOR,
                DISPLAY_ORDER
        );
    }

    private BadgeProgress createProgress(
            GamificationProfile profile,
            Badge badge,
            int currentValue,
            boolean completed
    ) {
        BadgeProgress progress = new BadgeProgress(profile, badge);
        progress.setCurrentValue(currentValue);
        progress.setCompleted(completed);
        return progress;
    }

    private GamificationBadgeDTO createBadgeDto(boolean earned) {
        return new GamificationBadgeDTO(
                BADGE_CODE,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                "RARE",
                THEME_COLOR,
                "READING",
                REWARD_POINTS,
                earned,
                earned ? LocalDateTime.now() : null
        );
    }

    private GamificationProgressDTO createProgressDto(boolean completed) {
        return new GamificationProgressDTO(
                BADGE_CODE,
                BADGE_NAME,
                BADGE_DESCRIPTION,
                ASSET_KEY,
                LOCKED_ASSET_KEY,
                "RARE",
                THEME_COLOR,
                "READING",
                2,
                TARGET_VALUE,
                40,
                completed
        );
    }

    private GamificationRecentEventDTO createRecentEventDto() {
        return new GamificationRecentEventDTO(
                "REVIEW_CREATED",
                1,
                "Recensie geschreven",
                LocalDateTime.now()
        );
    }
}