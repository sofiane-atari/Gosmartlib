package edu.ap.bib_backend.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class BookAvailableEventTest {

    @Test
    void GivenEventFields_WhenCreateEvent_ThenFieldsAreAccessible() {
        Long bookId = 42L;
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        BookAvailableEvent event = new BookAvailableEvent(bookId, occurredAt);

        assertEquals(42L, event.bookId());
        assertEquals(occurredAt, event.occurredAt());
    }

    @Test
    void GivenTwoEventsWithSameValues_WhenEquals_ThenAreEqual() {
        LocalDateTime now = LocalDateTime.now();
        BookAvailableEvent event1 = new BookAvailableEvent(1L, now);
        BookAvailableEvent event2 = new BookAvailableEvent(1L, now);

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void GivenTwoEventsWithDifferentBookIds_WhenEquals_ThenAreNotEqual() {
        LocalDateTime now = LocalDateTime.now();
        BookAvailableEvent event1 = new BookAvailableEvent(1L, now);
        BookAvailableEvent event2 = new BookAvailableEvent(2L, now);

        assertNotEquals(event1, event2);
    }
}
