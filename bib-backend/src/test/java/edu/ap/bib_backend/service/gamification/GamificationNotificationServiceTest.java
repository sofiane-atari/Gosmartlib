package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationNotification;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.GamificationNotificationRepository;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;
import edu.ap.bib_backend.service.gamification.sse.GamificationNotificationSseService;
import edu.ap.bib_backend.service.user.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class GamificationNotificationServiceTest {

    private static final String USER_ID = "student-user-id";
    private static final Long PROFILE_ID = 1L;
    private static final Long OTHER_PROFILE_ID = 2L;
    private static final Long NOTIFICATION_ID = 10L;

    private static final String BADGE_CODE = "REVIEWER_5_REVIEWS";
    private static final String BADGE_NAME = "Recensie-expert";
    private static final String BADGE_DESCRIPTION = "Je hebt vijf recensies geschreven.";
    private static final String ASSET_KEY = "reviewer-5-reviews";
    private static final String LOCKED_ASSET_KEY = "reviewer-5-reviews-locked";
    private static final String THEME_COLOR = "#7C3AED";
    private static final int TARGET_VALUE = 5;
    private static final int REWARD_POINTS = 10;
    private static final int DISPLAY_ORDER = 5;

    @Mock
    private GamificationNotificationRepository notificationRepository;

    @Mock
    private GamificationProfileService gamificationProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private GamificationMapper gamificationMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private GamificationNotificationSseService gamificationNotificationSseService;

    @InjectMocks
    private GamificationNotificationService notificationService;

    @Test
    void givenUnlockedBadge_whenCreateBadgeUnlockedNotification_thenSavesNotificationWithBadgeData() {
        // Arrange
        GamificationProfile profile = createProfile(PROFILE_ID);
        Badge badge = createBadge();

        ArgumentCaptor<GamificationNotification> notificationCaptor =
                ArgumentCaptor.forClass(GamificationNotification.class);

        // Act
        notificationService.createBadgeUnlockedNotification(profile, badge);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());

        GamificationNotification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getProfile()).isSameAs(profile);
        assertThat(savedNotification.getTitle()).isEqualTo("Badge behaald");
        assertThat(savedNotification.getMessage()).contains(BADGE_NAME);
        assertThat(savedNotification.getAssetKey()).isEqualTo(ASSET_KEY);
        assertThat(savedNotification.getThemeColor()).isEqualTo(THEME_COLOR);
        assertThat(savedNotification.getRarity()).isEqualTo(BadgeRarity.EPIC);
        assertThat(savedNotification.getRewardPoints()).isEqualTo(REWARD_POINTS);
        assertThat(savedNotification.getReadAt()).isNull();
    }

    @Test
    void givenUnreadNotifications_whenGetUnreadNotifications_thenReturnsMappedDtos() {
        // Arrange
        User user = new User();
        GamificationProfile profile = createProfile(PROFILE_ID);
        GamificationNotification notification = createNotification(profile);
        GamificationNotificationDTO dto = createNotificationDto();

        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);
        when(notificationRepository.findTop5ByProfileAndReadAtIsNullOrderByCreatedAtDesc(profile))
                .thenReturn(List.of(notification));
        when(gamificationMapper.toNotificationDTO(notification)).thenReturn(dto);

        // Act
        List<GamificationNotificationDTO> result =
                notificationService.getUnreadNotifications(authentication);

        // Assert
        assertThat(result).containsExactly(dto);
    }

    @Test
    void givenOwnUnreadNotification_whenMarkAsRead_thenNotificationIsMarkedAsRead() {
        // Arrange
        User user = new User();
        GamificationProfile profile = createProfile(PROFILE_ID);
        GamificationNotification notification = createNotification(profile);

        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(profile);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        // Act
        notificationService.markAsRead(authentication, NOTIFICATION_ID);

        // Assert
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void givenNotificationFromOtherProfile_whenMarkAsRead_thenThrowsResourceNotFoundException() {
        // Arrange
        User user = new User();
        GamificationProfile currentProfile = createProfile(PROFILE_ID);
        GamificationProfile otherProfile = createProfile(OTHER_PROFILE_ID);
        GamificationNotification notification = createNotification(otherProfile);

        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileService.getOrCreateProfile(user)).thenReturn(currentProfile);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.markAsRead(authentication, NOTIFICATION_ID)
        );

        // Assert
        assertThat(exception).isNotNull();
    }

    @Test
    void givenUnlockedBadge_whenCreateBadgeUnlockedNotification_thenSendsNotificationToUser() {
        // Arrange
        GamificationProfile profile = createProfile(PROFILE_ID);
        Badge badge = createBadge();
        GamificationNotification notification = createNotification(profile);
        GamificationNotificationDTO dto = createNotificationDto();

        when(notificationRepository.save(any(GamificationNotification.class)))
                .thenReturn(notification);
        when(gamificationMapper.toNotificationDTO(notification)).thenReturn(dto);

        // Act
        notificationService.createBadgeUnlockedNotification(profile, badge);

        // Assert
        verify(gamificationNotificationSseService).sendNotificationToUser(USER_ID, dto);
    }

    private GamificationProfile createProfile(Long id) {
        GamificationProfile profile = new GamificationProfile();
        profile.setId(id);
        User user = new User();
        user.setId(USER_ID);
        profile.setUser(user);
        return profile;
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

    private GamificationNotification createNotification(GamificationProfile profile) {
        return new GamificationNotification(
                profile,
                edu.ap.bib_backend.jpa.enums.gamification.GamificationNotificationType.BADGE_UNLOCKED,
                "Badge behaald",
                "Je hebt de badge '" + BADGE_NAME + "' ontgrendeld.",
                ASSET_KEY,
                THEME_COLOR,
                BadgeRarity.EPIC,
                REWARD_POINTS
        );
    }

    private GamificationNotificationDTO createNotificationDto() {
        return new GamificationNotificationDTO(
                NOTIFICATION_ID,
                "BADGE_UNLOCKED",
                "Badge behaald",
                "Je hebt de badge '" + BADGE_NAME + "' ontgrendeld.",
                ASSET_KEY,
                THEME_COLOR,
                "EPIC",
                REWARD_POINTS,
                java.time.LocalDateTime.now()
        );
    }
}
