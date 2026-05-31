package edu.ap.bib_backend.event;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.MailSubscriptionService;
import edu.ap.bib_backend.service.SmartschoolMessageService;
import edu.ap.bib_backend.service.SmartschoolTokenRefreshService;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.component.BookAvailableMessageComponent;
import edu.ap.bib_backend.service.message.component.BookRecommendationComponent;

@Component
public class BookAvailableEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookAvailableEventListener.class);

    private final MailSubscriptionService mailSubscriptionService;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    private final String frontendUrl;

    public BookAvailableEventListener(
            MailSubscriptionService mailSubscriptionService,
            BookRepository bookRepository,
            BookService bookService,
            SmartschoolMessageService smartschoolMessageService,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.mailSubscriptionService = mailSubscriptionService;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookAvailable(BookAvailableEvent event) {
        Book book = bookRepository.findById(event.bookId()).orElse(null);
        if (book == null) {
            log.warn("BookAvailableEvent received for unknown book id: {}", event.bookId());
            return;
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            log.debug("Book '{}' (id={}) has no available copies, skipping notification", book.getTitle(),
                    book.getId());
            return;
        }

        List<MailSubscription> subscribers = mailSubscriptionService.findSubscriptionsByBookId(event.bookId());
        if (subscribers.isEmpty()) {
            log.debug("No subscribers for book '{}' (id={})", book.getTitle(), book.getId());
            return;
        }

        Set<Long> schoolIdsWithCopies = book.getBookCopies().stream()
                .map(copy -> copy.getCampus().getSchool().getId())
                .collect(Collectors.toSet());

        log.info("Notifying subscribers that book '{}' is available (schools with copies: {})", book.getTitle(), schoolIdsWithCopies);

        for (MailSubscription subscription : subscribers) {
            String userId = subscription.getUser().getId();
            Long userSchoolId = subscription.getUser().getSchool().getId();

            if (!schoolIdsWithCopies.contains(userSchoolId)) {
                log.debug("Skipping user {} from different school", userId);
                continue;
            }

            try {
                String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(userId);
                if (token == null || token.isBlank()) {
                    log.warn("No valid Smartschool token for user {}, skipping notification", userId);
                    continue;
                }

                Optional<String> schoolDomain = book.getBookCopies().stream()
                        .map(copy -> copy.getCampus().getSchool().getDomainName())
                        .filter(Objects::nonNull)
                        .findFirst();
                List<BookDTO> recommendations = schoolDomain
                        .map(domain -> bookService.getRelatedBooksForLoan(List.of(book.getId()), 4, domain))
                        .orElseGet(List::of);

                String htmlMessage = new HtmlMessageBuilder(
                        "Boek beschikbaar", frontendUrl)
                        .addComponent(new BookAvailableMessageComponent(book, frontendUrl))
                        .addComponent(new BookRecommendationComponent(recommendations, frontendUrl))
                        .build();

                smartschoolMessageService.sendMessage(token, book.getTitle() + " is beschikbaar!", htmlMessage);
                log.info("Notified user {} about book '{}'", userId, book.getTitle());
            } catch (Exception e) {
                log.warn("Failed to notify user {} about book '{}': {}", userId, book.getTitle(), e.getMessage());
            }
        }
    }
}
