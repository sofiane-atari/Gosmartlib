package edu.ap.bib_backend.jpa.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MailTypeTest {

    @Test
    void GivenMailTypeEnum_WhenGetValue_ThenReturnsAvailableBook() {
        assertEquals("AVAILABLE_BOOK", MailType.AVAILABLE_BOOK.name());
    }

    @Test
    void GivenMailTypeEnum_WhenValueOf_ThenReturnsCorrectConstant() {
        MailType type = MailType.valueOf("AVAILABLE_BOOK");
        assertEquals(MailType.AVAILABLE_BOOK, type);
    }
}
