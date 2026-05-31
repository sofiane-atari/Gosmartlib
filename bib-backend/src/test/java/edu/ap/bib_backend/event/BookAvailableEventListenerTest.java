package edu.ap.bib_backend.event;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.MailSubscriptionService;
import edu.ap.bib_backend.service.SmartschoolMessageService;
import edu.ap.bib_backend.service.SmartschoolTokenRefreshService;
import edu.ap.bib_backend.service.book.BookService;

@ExtendWith(MockitoExtension.class)
class BookAvailableEventListenerTest {

    private static final Long BOOK_ID = 1L;
    private static final Long CAMPUS_ID = 10L;
    private static final Long SCHOOL_ID = 1L;
    private static final String USER_ID = "user1";
    private static final String SCHOOL_DOMAIN = "school.be";

    @Mock
    private MailSubscriptionService mailSubscriptionService;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookService bookService;
    @Mock
    private SmartschoolMessageService smartschoolMessageService;
    @Mock
    private SmartschoolTokenRefreshService smartschoolTokenRefreshService;

    @InjectMocks
    private BookAvailableEventListener eventListener;

    private Book createBook(Long id, String title, int availableCopies, Long campusId) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("Author Name");
        book.setDescription("A great book description");
        book.setImageUrl("https://example.com/cover.jpg");
        book.setAvailableCopies(availableCopies);
        Campus campus = new Campus();
        campus.setId(campusId);
        School school = new School();
        school.setId(SCHOOL_ID);
        school.setDomainName(SCHOOL_DOMAIN);
        campus.setSchool(school);
        BookCopy copy = new BookCopy();
        copy.setCampus(campus);
        copy.setBook(book);
        book.getBookCopies().add(copy);
        return book;
    }

    private MailSubscription createSubscription(String userId, Long schoolId) {
        User user = new User();
        user.setId(userId);
        School school = new School();
        school.setId(schoolId);
        school.setDomainName(SCHOOL_DOMAIN);
        user.setSchool(school);
        MailSubscription sub = new MailSubscription();
        sub.setUser(user);
        return sub;
    }

    @Test
    void GivenUnknownBookId_WhenOnBookAvailable_ThenLogsWarningAndReturns() {
        BookAvailableEvent event = new BookAvailableEvent(99L, LocalDateTime.now());
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        eventListener.onBookAvailable(event);

        then(bookRepository).should().findById(99L);
        then(mailSubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    void GivenBookWithNoAvailableCopies_WhenOnBookAvailable_ThenSkipsNotification() {
        Book book = createBook(BOOK_ID, "Unavailable Book", 0, CAMPUS_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));

        eventListener.onBookAvailable(event);

        then(mailSubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    void GivenBookWithNullAvailableCopies_WhenOnBookAvailable_ThenSkipsNotification() {
        Book book = createBook(BOOK_ID, "Null Copies Book", 1, CAMPUS_ID);
        book.setAvailableCopies(null);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));

        eventListener.onBookAvailable(event);

        then(mailSubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    void GivenBookWithNoSubscribers_WhenOnBookAvailable_ThenSkipsNotification() {
        Book book = createBook(BOOK_ID, "No Subscribers Book", 3, CAMPUS_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of());

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).shouldHaveNoInteractions();
    }

    @Test
    void GivenSubscriberFromDifferentCampus_WhenOnBookAvailable_ThenSkipsThatSubscriber() {
        Book book = createBook(BOOK_ID, "Different Campus Book", 3, CAMPUS_ID);
        MailSubscription sub = createSubscription(USER_ID, 99L);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub));

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).shouldHaveNoInteractions();
    }

    @Test
    void GivenSubscriberWithNoValidToken_WhenOnBookAvailable_ThenSkipsThatSubscriber() {
        Book book = createBook(BOOK_ID, "No Token Book", 3, CAMPUS_ID);
        MailSubscription sub = createSubscription(USER_ID, SCHOOL_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub));
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(USER_ID)).willReturn(null);

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).shouldHaveNoInteractions();
    }

    @Test
    void GivenSubscriberWithBlankToken_WhenOnBookAvailable_ThenSkipsThatSubscriber() {
        Book book = createBook(BOOK_ID, "Blank Token Book", 3, CAMPUS_ID);
        MailSubscription sub = createSubscription(USER_ID, SCHOOL_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub));
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(USER_ID)).willReturn("");

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).shouldHaveNoInteractions();
    }

    @Test
    void GivenValidSubscriber_WhenOnBookAvailable_ThenSendsEmail() {
        Book book = createBook(BOOK_ID, "Available Book", 3, CAMPUS_ID);
        MailSubscription sub = createSubscription(USER_ID, SCHOOL_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());
        BookDTO recommendation = new BookDTO();
        recommendation.setId(2L);
        recommendation.setTitle("Recommended Book");

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub));
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(USER_ID)).willReturn("valid-token");
        given(bookService.getRelatedBooksForLoan(List.of(BOOK_ID), 4, SCHOOL_DOMAIN))
                .willReturn(List.of(recommendation));

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).should()
                .sendMessage(eq("valid-token"), eq("Available Book is beschikbaar!"), anyString());
    }

    @Test
    void GivenMultipleSubscribers_WhenOnBookAvailable_ThenNotifiesAllWithValidToken() {
        Book book = createBook(BOOK_ID, "Multiple Subscribers Book", 3, CAMPUS_ID);
        MailSubscription sub1 = createSubscription("user1", SCHOOL_ID);
        MailSubscription sub2 = createSubscription("user2", SCHOOL_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub1, sub2));
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user1")).willReturn("token1");
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user2")).willReturn("token2");
        given(bookService.getRelatedBooksForLoan(List.of(BOOK_ID), 4, SCHOOL_DOMAIN))
                .willReturn(List.of());

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).should().sendMessage(eq("token1"), anyString(), anyString());
        then(smartschoolMessageService).should().sendMessage(eq("token2"), anyString(), anyString());
    }

    @Test
    void GivenExceptionDuringSending_WhenOnBookAvailable_ThenContinuesToNextSubscriber() {
        Book book = createBook(BOOK_ID, "Partial Failure Book", 3, CAMPUS_ID);
        MailSubscription sub1 = createSubscription("user1", SCHOOL_ID);
        MailSubscription sub2 = createSubscription("user2", SCHOOL_ID);
        BookAvailableEvent event = new BookAvailableEvent(BOOK_ID, LocalDateTime.now());

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(mailSubscriptionService.findSubscriptionsByBookId(BOOK_ID)).willReturn(List.of(sub1, sub2));
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user1")).willReturn("token1");
        given(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("user2")).willReturn("token2");
        given(bookService.getRelatedBooksForLoan(List.of(BOOK_ID), 4, SCHOOL_DOMAIN))
                .willReturn(List.of());
        doThrow(new RuntimeException("SMTP error")).doNothing()
                .when(smartschoolMessageService).sendMessage(anyString(), anyString(), anyString());

        eventListener.onBookAvailable(event);

        then(smartschoolMessageService).should(times(2)).sendMessage(anyString(), anyString(), anyString());
    }

}
