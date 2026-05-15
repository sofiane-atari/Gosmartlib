package edu.ap.bib_backend.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.MailSubscriptionService;
import edu.ap.bib_backend.service.SmartschoolMessageService;
import edu.ap.bib_backend.service.SmartschoolTokenRefreshService;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

@Component
public class BookAvailableEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookAvailableEventListener.class);

    private final MailSubscriptionService mailSubscriptionService;
    private final BookRepository bookRepository;
    private final SmartschoolMessageService smartschoolMessageService;
    private final SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    private final String frontendUrl;

    public BookAvailableEventListener(
            MailSubscriptionService mailSubscriptionService,
            BookRepository bookRepository,
            SmartschoolMessageService smartschoolMessageService,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.mailSubscriptionService = mailSubscriptionService;
        this.bookRepository = bookRepository;
        this.smartschoolMessageService = smartschoolMessageService;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.frontendUrl = frontendUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookAvailable(BookAvailableEvent event) {
        Book book = bookRepository.findById(event.bookId()).orElse(null);
        if (book == null) {
            log.warn("BookAvailableEvent received for unknown book id: {}", event.bookId());
            return;
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            log.debug("Book '{}' (id={}) has no available copies, skipping notification", book.getTitle(), book.getId());
            return;
        }

        List<MailSubscription> subscribers = mailSubscriptionService.findSubscriptionsByBookId(event.bookId());
        if (subscribers.isEmpty()) {
            log.debug("No subscribers for book '{}' (id={})", book.getTitle(), book.getId());
            return;
        }

        log.info("Notifying {} subscribers that book '{}' is available", subscribers.size(), book.getTitle());

        for (MailSubscription subscription : subscribers) {
            String userId = subscription.getUser().getId();
            try {
                String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(userId);
                if (token == null || token.isBlank()) {
                    log.warn("No valid Smartschool token for user {}, skipping notification", userId);
                    continue;
                }

                String htmlMessage = new HtmlMessageBuilder(
                        "Boek beschikbaar", frontendUrl)
                        .addComponent(new BookAvailableMessageComponent(book, frontendUrl))
                        .build();

                smartschoolMessageService.sendMessage(token, book.getTitle() + " is beschikbaar!", htmlMessage);
                log.info("Notified user {} about book '{}'", userId, book.getTitle());
            } catch (Exception e) {
                log.warn("Failed to notify user {} about book '{}': {}", userId, book.getTitle(), e.getMessage());
            }
        }
    }

    private static class BookAvailableMessageComponent implements MessageComponent {

        private final Book book;
        private final String frontendUrl;

        BookAvailableMessageComponent(Book book, String frontendUrl) {
            this.book = book;
            this.frontendUrl = frontendUrl;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String build() {
            String safeTitle = HtmlMessageBuilder.escapeHtml(book.getTitle());
            String safeAuthor = HtmlMessageBuilder.escapeHtml(book.getAuthor());

            return """
                    <div style="padding: 16px 0;">
                        <p style="margin: 0 0 12px 0; font-size: 16px; line-height: 1.5;">
                            Het boek <strong>%s</strong> van <strong>%s</strong> is opnieuw beschikbaar in de bibliotheek.
                        </p>
                        <p style="margin: 0 0 12px 0; font-size: 16px; line-height: 1.5;">
                            Je kan het boek komen lenen via de balie of online reserveren.
                        </p>
                        <p style="margin: 0; font-size: 16px; line-height: 1.5;">
                            <a href="%s/books/%d" style="color: #8a1e41; text-decoration: underline; font-weight: 600;">
                                Bekijk boek in catalogus
                            </a>
                        </p>
                    </div>
                    """.formatted(safeTitle, safeAuthor, frontendUrl, book.getId());
        }
    }
}
