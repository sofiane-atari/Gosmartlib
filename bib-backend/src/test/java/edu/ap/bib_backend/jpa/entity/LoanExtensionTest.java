package edu.ap.bib_backend.jpa.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.enums.LoanExtensionType;

class LoanExtensionTest {

    @Test
    void GivenAllFields_WhenCreateEntity_ThenFieldsAreSettableAndGettable() {
        Loan loan = new Loan();
        loan.setId(1L);

        LoanExtension extension = new LoanExtension();
        extension.setId(1L);
        extension.setLoan(loan);
        extension.setOldReturnDate(LocalDate.of(2026, 5, 1));
        extension.setNewReturnDate(LocalDate.of(2026, 5, 15));
        extension.setExtensionType(LoanExtensionType.CUSTOM_DATE);
        extension.setExtendedByUserId("librarian1");
        extension.setExtendedAt(LocalDateTime.of(2026, 4, 30, 14, 0));

        assertEquals(1L, extension.getId());
        assertEquals(1L, extension.getLoan().getId());
        assertEquals(LocalDate.of(2026, 5, 1), extension.getOldReturnDate());
        assertEquals(LocalDate.of(2026, 5, 15), extension.getNewReturnDate());
        assertEquals(LoanExtensionType.CUSTOM_DATE, extension.getExtensionType());
        assertEquals("librarian1", extension.getExtendedByUserId());
        assertEquals(LocalDateTime.of(2026, 4, 30, 14, 0), extension.getExtendedAt());
    }

    @Test
    void GivenNoArgsConstructor_WhenCreateEntity_ThenFieldsAreNull() {
        LoanExtension extension = new LoanExtension();

        assertNull(extension.getId());
        assertNull(extension.getLoan());
        assertNull(extension.getOldReturnDate());
        assertNull(extension.getNewReturnDate());
        assertNull(extension.getExtensionType());
        assertNull(extension.getExtendedByUserId());
        assertNull(extension.getExtendedAt());
    }
}
