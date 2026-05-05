package edu.ap.bib_backend.service.gamification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.exception.gamification.GamificationProfileNotFoundException;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.GamificationProfileRepository;


@Service
public class GamificationProfileService {

    private final GamificationProfileRepository gamificationProfileRepository;
    private final UserRepository userRepository;

    public GamificationProfileService(
            GamificationProfileRepository gamificationProfileRepository,
            UserRepository userRepository
    ) {
        this.gamificationProfileRepository = gamificationProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GamificationProfile getOrCreateProfile(User user) {
        return gamificationProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> createProfile(user));
    }

    @Transactional(readOnly = true)
    public GamificationProfile findByUserId(String userId) {
        return gamificationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GamificationProfileNotFoundException(userId));
    }

    @Transactional
    public GamificationProfile getOrCreateProfileByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GamificationProfileNotFoundException(userId));

        return getOrCreateProfile(user);
    }

    @Transactional
    public void addPoints(GamificationProfile profile, int points) {
        profile.setTotalPoints(profile.getTotalPoints() + points);
        updateLevel(profile);
        gamificationProfileRepository.save(profile);
    }

    private GamificationProfile createProfile(User user) {
        GamificationProfile profile = new GamificationProfile(user);

        return gamificationProfileRepository.save(profile);
    }

    private void updateLevel(GamificationProfile profile) {
        int totalPoints = profile.getTotalPoints();
        int currentLevel = calculateLevel(totalPoints);
        int pointsUntilNextLevel = calculatePointsUntilNextLevel(totalPoints, currentLevel);

        profile.setCurrentLevel(currentLevel);
        profile.setPointsUntilNextLevel(pointsUntilNextLevel);
    }

    private int calculateLevel(int totalPoints) {
        return (totalPoints / 100) + 1;
    }

    private int calculatePointsUntilNextLevel(int totalPoints, int currentLevel) {
        int nextLevelThreshold = currentLevel * 100;

        return nextLevelThreshold - totalPoints;
    }
}