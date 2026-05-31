package edu.ap.bib_backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.MailSubscriptionRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MailSubscriptionServiceIntegrationTest {

    @Autowired
    private MailSubscriptionService mailSubscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private MailSubscriptionRepository mailSubscriptionRepository;

    private User user;
    private Book book;
    private Campus campus;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        School school = new School();
        school.setDomainName("test.school.be");
        school.setName("Test School");
        school = schoolRepository.save(school);

        campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        RoleEntity role = roleEntityRepository.findByRole(Role.STUDENT)
                .orElseThrow(() -> new RuntimeException("Role STUDENT not found. Startup seeding must create it."));

        user = new User();
        user.setId("integration-test-user");
        user.setSchool(school);
        user.setRole(role);
        userRepository.save(user);

        book = new Book();
        book.setTitle("Integration Test Book");
        book.setIsbn("9780000000001");
        book = bookRepository.save(book);

        BookCopy bookCopy = new BookCopy();
        bookCopy.setBook(book);
        bookCopy.setCampus(campus);
        bookCopyRepository.save(bookCopy);
        book.getBookCopies().add(bookCopy);
    }

    @Test
    void GivenValidUserAndBook_WhenSubscribe_ThenSubscriptionIsPersistedAndReturned() {
        MailSubscriptionDTO result = mailSubscriptionService.subscribe(user.getId(), book.getId());

        assertNotNull(result);
        assertEquals(book.getId(), result.bookId());
        assertNotNull(result.id());
    }

    @Test
    void GivenExistingSubscription_WhenSubscribeAgain_ThenThrowsException() {
        mailSubscriptionService.subscribe(user.getId(), book.getId());

        assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.subscribe(user.getId(), book.getId()));
    }

    @Test
    void GivenExistingSubscription_WhenUnsubscribe_ThenRemovesSubscription() {
        mailSubscriptionService.subscribe(user.getId(), book.getId());
        mailSubscriptionService.unsubscribe(user.getId(), book.getId());

        List<MailSubscriptionDTO> subs = mailSubscriptionService.findSubscriptionsByUser(user.getId());
        assertTrue(subs.isEmpty());
    }

    @Test
    void GivenNoSubscription_WhenUnsubscribe_ThenThrowsException() {
        assertThrows(IllegalStateException.class,
                () -> mailSubscriptionService.unsubscribe(user.getId(), book.getId()));
    }

    @Test
    void GivenUserWithSubscriptions_WhenFindSubscriptionsByUser_ThenReturnsAll() {
        Book book2 = new Book();
        book2.setTitle("Second Book");
        book2.setIsbn("9780000000002");
        book2 = bookRepository.save(book2);

        BookCopy bookCopy2 = new BookCopy();
        bookCopy2.setBook(book2);
        bookCopy2.setCampus(campus);
        bookCopyRepository.save(bookCopy2);
        book2.getBookCopies().add(bookCopy2);

        mailSubscriptionService.subscribe(user.getId(), book.getId());
        mailSubscriptionService.subscribe(user.getId(), book2.getId());

        List<MailSubscriptionDTO> result = mailSubscriptionService.findSubscriptionsByUser(user.getId());

        assertEquals(2, result.size());
    }

    @Test
    void GivenUserWithNoSubscriptions_WhenFindSubscriptionsByUser_ThenReturnsEmpty() {
        List<MailSubscriptionDTO> result = mailSubscriptionService.findSubscriptionsByUser(user.getId());

        assertTrue(result.isEmpty());
    }

    private void cleanDatabase() {
        mailSubscriptionRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        campusRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
