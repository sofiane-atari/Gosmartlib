package edu.ap.bib_backend.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.MailSubscription;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.MailType;

class MailSubscriptionEntityTest {

    @Test
    void GivenAllFields_WhenCreateEntity_ThenFieldsAreSettableAndGettable() {
        User user = new User();
        user.setId("user1");
        Book book = new Book();
        book.setId(1L);

        MailSubscription subscription = new MailSubscription();
        subscription.setId(1L);
        subscription.setUser(user);
        subscription.setBook(book);
        subscription.setMailType(MailType.AVAILABLE_BOOK);
        subscription.setCreatedAt(LocalDateTime.of(2026, 5, 15, 10, 0));

        assertEquals(1L, subscription.getId());
        assertEquals("user1", subscription.getUser().getId());
        assertEquals(1L, subscription.getBook().getId());
        assertEquals(MailType.AVAILABLE_BOOK, subscription.getMailType());
        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 0), subscription.getCreatedAt());
    }

    @Test
    void GivenNoArgsConstructor_WhenCreateEntity_ThenFieldsAreNull() {
        MailSubscription subscription = new MailSubscription();

        assertNull(subscription.getId());
        assertNull(subscription.getUser());
        assertNull(subscription.getBook());
        assertNull(subscription.getMailType());
        assertNull(subscription.getCreatedAt());
    }

    @Test
    void GivenAllArgsConstructor_WhenCreateEntity_ThenFieldsAreSet() {
        User user = new User();
        user.setId("user1");
        Book book = new Book();
        book.setId(1L);
        LocalDateTime now = LocalDateTime.now();

        MailSubscription subscription = new MailSubscription(
                1L, user, book, MailType.AVAILABLE_BOOK, now);

        assertEquals(1L, subscription.getId());
        assertEquals("user1", subscription.getUser().getId());
        assertEquals(1L, subscription.getBook().getId());
        assertEquals(MailType.AVAILABLE_BOOK, subscription.getMailType());
        assertEquals(now, subscription.getCreatedAt());
    }
}
