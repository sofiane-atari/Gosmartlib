package edu.ap.bib_backend.event.gamification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.service.gamification.BadgeProgressService;
import edu.ap.bib_backend.service.gamification.GamificationProfileService;
import edu.ap.bib_backend.service.gamification.PointService;
import edu.ap.bib_backend.service.gamification.util.BadgeCode;
import edu.ap.bib_backend.service.gamification.util.GamificationSourceType;

@Service
public class GamificationEventService {

    private static final String BOOK_RETURNED_REASON = "Boek teruggebracht";
    private static final String BOOK_RETURNED_ON_TIME_REASON = "Boek op tijd teruggebracht";
    private static final String REVIEW_CREATED_REASON = "Recensie geschreven";

    private final GamificationProfileService gamificationProfileService;
    private final PointService pointService;
    private final BadgeProgressService badgeProgressService;

    public GamificationEventService(
            GamificationProfileService gamificationProfileService,
            PointService pointService,
            BadgeProgressService badgeProgressService
    ) {
        this.gamificationProfileService = gamificationProfileService;
        this.pointService = pointService;
        this.badgeProgressService = badgeProgressService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBookReturned(BookReturnedGamificationEvent event) {
        GamificationProfile profile = gamificationProfileService.getOrCreateProfileByUserId(event.userId());
        String sourceId = String.valueOf(event.loanId());

        boolean pointsAwarded = pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED,
                PointService.BOOK_RETURNED_POINTS,
                BOOK_RETURNED_REASON,
                GamificationSourceType.LOAN,
                sourceId
        );

        if (!pointsAwarded) {
            return;
        }

        badgeProgressService.increaseProgress(profile, BadgeCode.FIRST_BOOK_RETURNED, 1);
        badgeProgressService.increaseProgress(profile, BadgeCode.BOOKWORM_5_BOOKS, 1);
        badgeProgressService.increaseProgress(profile, BadgeCode.BOOK_RETURNER_10, 1);
        badgeProgressService.increaseProgress(profile, BadgeCode.LEGENDARY_READER_25, 1);

        if (event.returnedOnTime()) {
            handleBookReturnedOnTime(profile, sourceId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReviewCreated(ReviewCreatedGamificationEvent event) {
        GamificationProfile profile = gamificationProfileService.getOrCreateProfileByUserId(event.userId());
        String sourceId = String.valueOf(event.reviewId());

        boolean pointsAwarded = pointService.awardPoints(
                profile,
                PointTransactionType.REVIEW_CREATED,
                PointService.REVIEW_CREATED_POINTS,
                REVIEW_CREATED_REASON,
                GamificationSourceType.REVIEW,
                sourceId
        );

        if (!pointsAwarded) {
            return;
        }

        badgeProgressService.increaseProgress(profile, BadgeCode.FIRST_REVIEW, 1);
        badgeProgressService.increaseProgress(profile, BadgeCode.REVIEWER_5_REVIEWS, 1);
    }

    private void handleBookReturnedOnTime(GamificationProfile profile, String sourceId) {
        pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED_ON_TIME,
                PointService.BOOK_RETURNED_ON_TIME_BONUS_POINTS,
                BOOK_RETURNED_ON_TIME_REASON,
                GamificationSourceType.LOAN,
                sourceId
        );

        badgeProgressService.increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_5, 1);
        badgeProgressService.increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_10, 1);
    }
}