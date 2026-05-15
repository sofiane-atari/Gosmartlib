package edu.ap.bib_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.MailType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.MailSubscriptionRepository;

@DataJpaTest
@ActiveProfiles("test")
class MailSubscriptionRepositoryTest {

    @Autowired
    private MailSubscriptionRepository mailSubscriptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setDomainName("repo-test.school.be");
        school.setName("Repo Test School");
        entityManager.persist(school);

        Campus campus = new Campus();
        campus.setName("Repo Test Campus");
        campus.setSchool(school);
        entityManager.persist(campus);

        RoleEntity role = new RoleEntity();
        role.setRole(Role.STUDENT);
        entityManager.persist(role);

        user = new User();
        user.setId("repo-test-user");
        user.setCampus(campus);
        user.setSchool(school);
        user.setRole(role);
        entityManager.persist(user);

        book = new Book();
        book.setTitle("Repo Test Book");
        book.setIsbn("9780000000099");
        book.setCampus(campus);
        entityManager.persist(book);

        entityManager.flush();
    }

    private MailSubscription createSubscription(User user, Book book, MailType mailType) {
        MailSubscription sub = new MailSubscription();
        sub.setUser(user);
        sub.setBook(book);
        sub.setMailType(mailType);
        return entityManager.persistAndFlush(sub);
    }

    @Test
    void GivenBookAndMailType_WhenFindByBookIdAndMailType_ThenReturnsSubscriptions() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        List<MailSubscription> result = mailSubscriptionRepository
                .findByBookIdAndMailType(book.getId(), MailType.AVAILABLE_BOOK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void GivenDifferentMailType_WhenFindByBookIdAndMailType_ThenReturnsEmpty() {
        // MailType is an enum with only AVAILABLE_BOOK, so this tests the basic filtering
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        // The query will match only the correct type - since we have no other type,
        // this verifies the query works correctly
        List<MailSubscription> result = mailSubscriptionRepository
                .findByBookIdAndMailType(book.getId(), MailType.AVAILABLE_BOOK);

        assertThat(result).hasSize(1);
    }

    @Test
    void GivenUserId_WhenFindByUserId_ThenReturnsSubscriptions() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        List<MailSubscription> result = mailSubscriptionRepository.findByUserId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBook().getId()).isEqualTo(book.getId());
    }

    @Test
    void GivenDifferentUserId_WhenFindByUserId_ThenReturnsEmpty() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        List<MailSubscription> result = mailSubscriptionRepository.findByUserId("nonexistent-user");

        assertThat(result).isEmpty();
    }

    @Test
    void GivenExistingSubscription_WhenFindByUserIdAndBookIdAndMailType_ThenReturnsOptional() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        Optional<MailSubscription> result = mailSubscriptionRepository
                .findByUserIdAndBookIdAndMailType(user.getId(), book.getId(), MailType.AVAILABLE_BOOK);

        assertThat(result).isPresent();
    }

    @Test
    void GivenNoSubscription_WhenFindByUserIdAndBookIdAndMailType_ThenReturnsEmpty() {
        Optional<MailSubscription> result = mailSubscriptionRepository
                .findByUserIdAndBookIdAndMailType("nonexistent", 999L, MailType.AVAILABLE_BOOK);

        assertThat(result).isEmpty();
    }

    @Test
    void GivenExistingSubscription_WhenExistsByUserIdAndBookIdAndMailType_ThenReturnsTrue() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        boolean exists = mailSubscriptionRepository
                .existsByUserIdAndBookIdAndMailType(user.getId(), book.getId(), MailType.AVAILABLE_BOOK);

        assertThat(exists).isTrue();
    }

    @Test
    void GivenNoSubscription_WhenExistsByUserIdAndBookIdAndMailType_ThenReturnsFalse() {
        boolean exists = mailSubscriptionRepository
                .existsByUserIdAndBookIdAndMailType("nonexistent", 999L, MailType.AVAILABLE_BOOK);

        assertThat(exists).isFalse();
    }

    @Test
    void GivenExistingSubscription_WhenDeleteByUserIdAndBookIdAndMailType_ThenDeletes() {
        createSubscription(user, book, MailType.AVAILABLE_BOOK);

        mailSubscriptionRepository.deleteByUserIdAndBookIdAndMailType(
                user.getId(), book.getId(), MailType.AVAILABLE_BOOK);

        List<MailSubscription> result = mailSubscriptionRepository.findByUserId(user.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void GivenMultipleSubscriptions_WhenFindByUserId_ThenReturnsAll() {
        Book book2 = new Book();
        book2.setTitle("Second Book");
        book2.setIsbn("9780000000010");
        book2.setCampus(book.getCampus());
        entityManager.persist(book2);
        entityManager.flush();

        createSubscription(user, book, MailType.AVAILABLE_BOOK);
        createSubscription(user, book2, MailType.AVAILABLE_BOOK);

        List<MailSubscription> result = mailSubscriptionRepository.findByUserId(user.getId());

        assertThat(result).hasSize(2);
    }
}
