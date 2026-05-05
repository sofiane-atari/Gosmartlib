package edu.ap.bib_backend.service.gamification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.PointTransaction;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.jpa.repository.gamification.PointTransactionRepository;

@Service
public class PointService {

    public static final int BOOK_RETURNED_POINTS = 10;
    public static final int BOOK_RETURNED_ON_TIME_BONUS_POINTS = 5;
    public static final int REVIEW_CREATED_POINTS = 5;

    private final PointTransactionRepository pointTransactionRepository;
    private final GamificationProfileService gamificationProfileService;

    public PointService(
            PointTransactionRepository pointTransactionRepository,
            GamificationProfileService gamificationProfileService
    ) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.gamificationProfileService = gamificationProfileService;
    }

    @Transactional
    public boolean awardPoints(
            GamificationProfile profile,
            PointTransactionType type,
            int points,
            String reason,
            String sourceType,
            String sourceId
    ) {
        if (hasAlreadyAwardedPoints(profile, type, sourceType, sourceId)) {
            return false;
        }

        PointTransaction transaction = new PointTransaction(
                profile,
                type,
                points,
                reason,
                sourceType,
                sourceId
        );

        pointTransactionRepository.save(transaction);
        gamificationProfileService.addPoints(profile, points);

        return true;
    }

    @Transactional(readOnly = true)
    public boolean hasAlreadyAwardedPoints(
            GamificationProfile profile,
            PointTransactionType type,
            String sourceType,
            String sourceId
    ) {
        return pointTransactionRepository.existsByProfileAndTypeAndSourceTypeAndSourceId(
                profile,
                type,
                sourceType,
                sourceId
        );
    }
}
