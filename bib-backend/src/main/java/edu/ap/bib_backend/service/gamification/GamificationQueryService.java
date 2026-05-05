package edu.ap.bib_backend.service.gamification;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.gamification.GamificationBadgeDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.dto.gamification.GamificationRecentEventDTO;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.PointTransactionRepository;
import edu.ap.bib_backend.jpa.repository.gamification.UserBadgeRepository;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class GamificationQueryService {

    private final UserRepository userRepository;
    private final GamificationProfileService gamificationProfileService;
    private final BadgeService badgeService;
    private final BadgeProgressService badgeProgressService;
    private final UserBadgeRepository userBadgeRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final GamificationMapper gamificationMapper;
    private final CurrentUserService currentUserService;

    public GamificationQueryService(
            UserRepository userRepository,
            GamificationProfileService gamificationProfileService,
            BadgeService badgeService,
            BadgeProgressService badgeProgressService,
            UserBadgeRepository userBadgeRepository,
            PointTransactionRepository pointTransactionRepository,
            GamificationMapper gamificationMapper,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.gamificationProfileService = gamificationProfileService;
        this.badgeService = badgeService;
        this.badgeProgressService = badgeProgressService;
        this.userBadgeRepository = userBadgeRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.gamificationMapper = gamificationMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public GamificationProfileDTO getCurrentUserProfile(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        return getProfileForUserId(currentUserId);
    }

    @Transactional(readOnly = true)
    public GamificationProfileDTO getProfileForUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        GamificationProfile profile = gamificationProfileService.getOrCreateProfile(user);

        List<GamificationBadgeDTO> latestAchievements = getLatestAchievements(profile);
        List<GamificationBadgeDTO> earnedBadges = getEarnedBadges(profile);
        List<GamificationProgressDTO> badgeProgress = getBadgeProgress(profile);
        List<GamificationRecentEventDTO> recentEvents = getRecentEvents(profile);

        return new GamificationProfileDTO(
                profile.getTotalPoints(),
                profile.getCurrentLevel(),
                profile.getPointsUntilNextLevel(),
                latestAchievements,
                earnedBadges,
                badgeProgress,
                recentEvents
        );
    }

    private List<GamificationBadgeDTO> getLatestAchievements(GamificationProfile profile) {
        return userBadgeRepository.findTop3ByProfileOrderByEarnedAtDesc(profile)
                .stream()
                .map(gamificationMapper::toEarnedBadgeDTO)
                .toList();
    }

    private List<GamificationBadgeDTO> getEarnedBadges(GamificationProfile profile) {
        return userBadgeRepository.findByProfileOrderByEarnedAtDesc(profile)
                .stream()
                .map(gamificationMapper::toEarnedBadgeDTO)
                .toList();
    }

    private List<GamificationProgressDTO> getBadgeProgress(GamificationProfile profile) {
        List<Badge> activeBadges = badgeService.findAllActiveBadges();

        return activeBadges.stream()
                .map(badge -> {
                    BadgeProgress progress = getProgressForBadge(profile, badge);
                    return gamificationMapper.toProgressDTO(progress);
                })
                .toList();
    }

    private BadgeProgress getProgressForBadge(GamificationProfile profile, Badge badge) {
        return badgeProgressService.findProgress(profile, badge);
    }

    private List<GamificationRecentEventDTO> getRecentEvents(GamificationProfile profile) {
        return pointTransactionRepository.findTop10ByProfileOrderByCreatedAtDesc(profile)
                .stream()
                .map(gamificationMapper::toRecentEventDTO)
                .toList();
    }
}