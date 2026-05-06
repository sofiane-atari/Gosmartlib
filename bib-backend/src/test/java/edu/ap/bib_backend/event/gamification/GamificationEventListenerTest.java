package edu.ap.bib_backend.event.gamification;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.event.BookReturnedGamificationEvent;
import edu.ap.bib_backend.event.GamificationEventListener;
import edu.ap.bib_backend.event.GamificationEventService;
import edu.ap.bib_backend.event.ReviewCreatedGamificationEvent;

@ExtendWith(MockitoExtension.class)
class GamificationEventListenerTest {

    private static final String USER_ID = "student-user-id";
    private static final Long LOAN_ID = 12L;
    private static final Long REVIEW_ID = 34L;
    private static final Long BOOK_ID = 56L;

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 5, 10);
    private static final LocalDate RETURNED_DATE = LocalDate.of(2026, 5, 9);
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 9, 12, 0);

    @Mock
    private GamificationEventService gamificationEventService;

    @InjectMocks
    private GamificationEventListener gamificationEventListener;

    @Test
    void givenBookReturnedEvent_whenHandleBookReturned_thenDelegatesToEventService() {
        // Arrange
        BookReturnedGamificationEvent event = new BookReturnedGamificationEvent(
                USER_ID,
                LOAN_ID,
                true,
                DUE_DATE,
                RETURNED_DATE,
                OCCURRED_AT
        );

        // Act
        gamificationEventListener.handleBookReturned(event);

        // Assert
        verify(gamificationEventService).handleBookReturned(event);
    }

    @Test
    void givenReviewCreatedEvent_whenHandleReviewCreated_thenDelegatesToEventService() {
        // Arrange
        ReviewCreatedGamificationEvent event = new ReviewCreatedGamificationEvent(
                USER_ID,
                REVIEW_ID,
                BOOK_ID,
                OCCURRED_AT
        );

        // Act
        gamificationEventListener.handleReviewCreated(event);

        // Assert
        verify(gamificationEventService).handleReviewCreated(event);
    }
}