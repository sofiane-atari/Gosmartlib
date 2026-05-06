package edu.ap.bib_backend.event.gamification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.event.GamificationEventService;
import edu.ap.bib_backend.event.ReviewCreatedGamificationEvent;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.service.gamification.BadgeProgressService;
import edu.ap.bib_backend.service.gamification.GamificationProfileService;
import edu.ap.bib_backend.service.gamification.PointService;
import edu.ap.bib_backend.service.gamification.util.BadgeCode;
import edu.ap.bib_backend.service.gamification.util.GamificationSourceType;

@ExtendWith(MockitoExtension.class)
class GamificationEventServiceTest {

    private static final String USER_ID = "student-user-id";
    private static final Long LOAN_ID = 12L;
    private static final Long REVIEW_ID = 34L;
    private static final Long BOOK_ID = 56L;

    private static final String LOAN_SOURCE_ID = "12";
    private static final String REVIEW_SOURCE_ID = "34";

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 5, 10);
    private static final LocalDate RETURNED_DATE = LocalDate.of(2026, 5, 9);
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 9, 12, 0);

    @Mock
    private GamificationProfileService gamificationProfileService;

    @Mock
    private PointService pointService;

    @Mock
    private BadgeProgressService badgeProgressService;

    @InjectMocks
    private GamificationEventService gamificationEventService;

    @Test
    void givenNewBookReturnedEvent_whenHandleBookReturned_thenAwardsPointsAndIncreasesReadingBadgeProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        BookReturnedGamificationEvent event = createBookReturnedEvent(false);

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED,
                PointService.BOOK_RETURNED_POINTS,
                "Boek teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        )).thenReturn(true);

        // Act
        gamificationEventService.handleBookReturned(event);

        // Assert
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.FIRST_BOOK_RETURNED, 1);
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.BOOKWORM_5_BOOKS, 1);
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.BOOK_RETURNER_10, 1);
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.LEGENDARY_READER_25, 1);

        verify(pointService, never()).awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED_ON_TIME,
                PointService.BOOK_RETURNED_ON_TIME_BONUS_POINTS,
                "Boek op tijd teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        );

        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_5, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_10, 1);
    }

    @Test
    void givenDuplicateBookReturnedEvent_whenHandleBookReturned_thenDoesNotIncreaseBadgeProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        BookReturnedGamificationEvent event = createBookReturnedEvent(false);

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED,
                PointService.BOOK_RETURNED_POINTS,
                "Boek teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        )).thenReturn(false);

        // Act
        gamificationEventService.handleBookReturned(event);

        // Assert
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.FIRST_BOOK_RETURNED, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.BOOKWORM_5_BOOKS, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.BOOK_RETURNER_10, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.LEGENDARY_READER_25, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_5, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_10, 1);
    }

    @Test
    void givenOnTimeBookReturnedEvent_whenHandleBookReturned_thenAwardsBonusAndIncreasesOnTimeBadgeProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        BookReturnedGamificationEvent event = createBookReturnedEvent(true);

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED,
                PointService.BOOK_RETURNED_POINTS,
                "Boek teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        )).thenReturn(true);

        // Act
        gamificationEventService.handleBookReturned(event);

        // Assert
        verify(pointService).awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED_ON_TIME,
                PointService.BOOK_RETURNED_ON_TIME_BONUS_POINTS,
                "Boek op tijd teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        );

        verify(badgeProgressService).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_5, 1);
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_10, 1);
    }

    @Test
    void givenNewReviewCreatedEvent_whenHandleReviewCreated_thenAwardsPointsAndIncreasesReviewBadgeProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        ReviewCreatedGamificationEvent event = createReviewCreatedEvent();

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.REVIEW_CREATED,
                PointService.REVIEW_CREATED_POINTS,
                "Recensie geschreven",
                GamificationSourceType.REVIEW,
                REVIEW_SOURCE_ID
        )).thenReturn(true);

        // Act
        gamificationEventService.handleReviewCreated(event);

        // Assert
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.FIRST_REVIEW, 1);
        verify(badgeProgressService).increaseProgress(profile, BadgeCode.REVIEWER_5_REVIEWS, 1);
    }

    @Test
    void givenDuplicateReviewCreatedEvent_whenHandleReviewCreated_thenDoesNotIncreaseReviewBadgeProgress() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        ReviewCreatedGamificationEvent event = createReviewCreatedEvent();

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.REVIEW_CREATED,
                PointService.REVIEW_CREATED_POINTS,
                "Recensie geschreven",
                GamificationSourceType.REVIEW,
                REVIEW_SOURCE_ID
        )).thenReturn(false);

        // Act
        gamificationEventService.handleReviewCreated(event);

        // Assert
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.FIRST_REVIEW, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.REVIEWER_5_REVIEWS, 1);
    }

    @Test
    void givenOnTimeBookReturnedEvent_whenBaseReturnPointsAlreadyAwarded_thenDoesNotAwardOnTimeBonus() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();
        BookReturnedGamificationEvent event = createBookReturnedEvent(true);

        when(gamificationProfileService.getOrCreateProfileByUserId(USER_ID))
                .thenReturn(profile);

        when(pointService.awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED,
                PointService.BOOK_RETURNED_POINTS,
                "Boek teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        )).thenReturn(false);

        // Act
        gamificationEventService.handleBookReturned(event);

        // Assert
        verify(pointService, never()).awardPoints(
                profile,
                PointTransactionType.BOOK_RETURNED_ON_TIME,
                PointService.BOOK_RETURNED_ON_TIME_BONUS_POINTS,
                "Boek op tijd teruggebracht",
                GamificationSourceType.LOAN,
                LOAN_SOURCE_ID
        );

        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_5, 1);
        verify(badgeProgressService, never()).increaseProgress(profile, BadgeCode.ON_TIME_RETURNER_10, 1);
    }

    private BookReturnedGamificationEvent createBookReturnedEvent(boolean returnedOnTime) {
        return new BookReturnedGamificationEvent(
                USER_ID,
                LOAN_ID,
                returnedOnTime,
                DUE_DATE,
                RETURNED_DATE,
                OCCURRED_AT
        );
    }

    private ReviewCreatedGamificationEvent createReviewCreatedEvent() {
        return new ReviewCreatedGamificationEvent(
                USER_ID,
                REVIEW_ID,
                BOOK_ID,
                OCCURRED_AT
        );
    }
}