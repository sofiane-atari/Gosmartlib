package edu.ap.bib_backend.service.gamification;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.gamification.GamificationBadgeDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.dto.gamification.GamificationRecentEventDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.entity.gamification.BadgeProgress;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeProgressRepository;
import edu.ap.bib_backend.jpa.repository.gamification.PointTransactionRepository;
import edu.ap.bib_backend.jpa.repository.gamification.UserBadgeRepository;
import edu.ap.bib_backend.mapper.gamification.GamificationMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class GamificationQueryService {

    private static final int ALMOST_EARNED_BADGE_LIMIT = 3;

    private final UserRepository userRepository;
    private final GamificationProfileService gamificationProfileService;
    private final BadgeService badgeService;
    private final BadgeProgressService badgeProgressService;
    private final BadgeProgressRepository badgeProgressRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final GamificationMapper gamificationMapper;
    private final CurrentUserService currentUserService;

    public GamificationQueryService(
            UserRepository userRepository,
            GamificationProfileService gamificationProfileService,
            BadgeService badgeService,
            BadgeProgressService badgeProgressService,
            BadgeProgressRepository badgeProgressRepository,
            UserBadgeRepository userBadgeRepository,
            PointTransactionRepository pointTransactionRepository,
            GamificationMapper gamificationMapper,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.gamificationProfileService = gamificationProfileService;
        this.badgeService = badgeService;
        this.badgeProgressService = badgeProgressService;
        this.badgeProgressRepository = badgeProgressRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.gamificationMapper = gamificationMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GamificationProfileDTO getCurrentUserProfile(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        return getProfileForUserId(currentUserId);
    }

    @Transactional
    public GamificationProfileDTO getProfileForUserId(String userId) {
        GamificationProfile profile = getOrCreateProfileForUserId(userId);

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

    @Transactional
    public List<GamificationProgressDTO> getCurrentUserBadges(
            Authentication authentication,
            BadgeCategory category,
            BadgeRarity rarity,
            Boolean completed
    ) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        return getBadgesForUserId(currentUserId, category, rarity, completed);
    }

    @Transactional
    public List<GamificationProgressDTO> getBadgesForUserId(
            String userId,
            BadgeCategory category,
            BadgeRarity rarity,
            Boolean completed
    ) {
        GamificationProfile profile = getOrCreateProfileForUserId(userId);

        return badgeService.findActiveBadges(category, rarity)
                .stream()
                .map(badge -> getProgressForBadge(profile, badge))
                .filter(progress -> matchesCompletedFilter(progress, completed))
                .map(gamificationMapper::toProgressDTO)
                .toList();
    }

    @Transactional
    public List<GamificationProgressDTO> getCurrentUserAlmostEarnedBadges(Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        return getAlmostEarnedBadgesForUserId(currentUserId);
    }

    @Transactional
    public List<GamificationProgressDTO> getAlmostEarnedBadgesForUserId(String userId) {
        GamificationProfile profile = getOrCreateProfileForUserId(userId);

        List<BadgeProgress> almostEarnedBadges = badgeProgressRepository.findAlmostEarnedBadgesByProfile(
                profile,
                PageRequest.of(0, ALMOST_EARNED_BADGE_LIMIT)
        );

        return almostEarnedBadges.stream()
                .map(gamificationMapper::toProgressDTO)
                .toList();
    }

    private GamificationProfile getOrCreateProfileForUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "userId",
                        "Gebruiker niet gevonden met id: " + userId
                ));

        return gamificationProfileService.getOrCreateProfile(user);
    }

    private boolean matchesCompletedFilter(BadgeProgress progress, Boolean completed) {
        return completed == null || progress.isCompleted() == completed;
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