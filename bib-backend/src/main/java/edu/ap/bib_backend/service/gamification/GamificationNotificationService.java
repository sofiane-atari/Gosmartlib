package edu.ap.bib_backend.service.gamification;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationNotification;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.GamificationNotificationType;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.GamificationNotificationRepository;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;
import edu.ap.bib_backend.service.gamification.sse.GamificationNotificationSseService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.springframework.security.core.Authentication;


@Service
public class GamificationNotificationService {

    private static final String BADGE_UNLOCKED_TITLE = "Badge behaald";

    private final GamificationNotificationRepository notificationRepository;
    private final GamificationProfileService gamificationProfileService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final GamificationMapper gamificationMapper;
    private final GamificationNotificationSseService gamificationNotificationSseService;

    public GamificationNotificationService(
        GamificationNotificationRepository notificationRepository,
        GamificationProfileService gamificationProfileService,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        GamificationMapper gamificationMapper,
        GamificationNotificationSseService gamificationNotificationSseService
        ) {
        this.notificationRepository = notificationRepository;
        this.gamificationProfileService = gamificationProfileService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.gamificationMapper = gamificationMapper;
        this.gamificationNotificationSseService = gamificationNotificationSseService;
        }

    @Transactional
    public void createBadgeUnlockedNotification(
        GamificationProfile profile,
        Badge badge
    ) {
    GamificationNotification notification = new GamificationNotification(
            profile,
            GamificationNotificationType.BADGE_UNLOCKED,
            BADGE_UNLOCKED_TITLE,
            "Je hebt de badge '" + badge.getName() + "' ontgrendeld.",
            badge.getAssetKey(),
            badge.getThemeColor(),
            badge.getRarity(),
            badge.getRewardPoints()
    );

    GamificationNotification savedNotification =
            notificationRepository.save(notification);

    GamificationNotificationDTO notificationDTO =
            gamificationMapper.toNotificationDTO(savedNotification);

    gamificationNotificationSseService.sendNotificationToUser(
            profile.getUser().getId(),
            notificationDTO
    );
}

    @Transactional
    public List<GamificationNotificationDTO> getUnreadNotifications(
            Authentication authentication
    ) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);
        GamificationProfile profile = getProfileForUserId(currentUserId);

        return notificationRepository
                .findTop5ByProfileAndReadAtIsNullOrderByCreatedAtDesc(profile)
                .stream()
                .map(gamificationMapper::toNotificationDTO)
                .toList();
    }

    @Transactional
    public void markAsRead(Authentication authentication, Long notificationId) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);
        GamificationProfile profile = getProfileForUserId(currentUserId);

        GamificationNotification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "notificationId",
                        "Notificatie niet gevonden met id: " + notificationId
                ));

        if (!notification.getProfile().getId().equals(profile.getId())) {
            throw new ResourceNotFoundException(
                    "notificationId",
                    "Notificatie niet gevonden met id: " + notificationId
            );
        }

        notification.markAsRead();
    }

    private GamificationProfile getProfileForUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "userId",
                        "Gebruiker niet gevonden met id: " + userId
                ));

        return gamificationProfileService.getOrCreateProfile(user);
    }

    @Transactional(readOnly = true)
    public String getCurrentUserId(Authentication authentication) {
        return currentUserService.extractCurrentUserId(authentication);
    }
}