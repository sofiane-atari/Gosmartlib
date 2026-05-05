package edu.ap.bib_backend.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GamificationEventListener {

    private final GamificationEventService gamificationEventService;

    public GamificationEventListener(GamificationEventService gamificationEventService) {
        this.gamificationEventService = gamificationEventService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookReturned(BookReturnedGamificationEvent event) {
        gamificationEventService.handleBookReturned(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedGamificationEvent event) {
        gamificationEventService.handleReviewCreated(event);
    }
}