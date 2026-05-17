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
import edu.ap.bib_backend.jpa.repository.CampusRepository;
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
    private RoleEntityRepository roleEntityRepository;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setDomainName("test.school.be");
        school.setName("Test School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        RoleEntity role = new RoleEntity();
        role.setRole(Role.STUDENT);
        roleEntityRepository.save(role);

        user = new User();
        user.setId("integration-test-user");
        user.setCampus(campus);
        user.setSchool(school);
        user.setRole(role);
        userRepository.save(user);

        book = new Book();
        book.setTitle("Integration Test Book");
        book.setIsbn("9780000000001");
        book.setCampus(campus);
        book = bookRepository.save(book);
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
        MailSubscriptionDTO sub = mailSubscriptionService.subscribe(user.getId(), book.getId());

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
        book2.setCampus(book.getCampus());
        book2 = bookRepository.save(book2);

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
}
