package edu.ap.bib_backend.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationErrorTest {

    @Test
    void constructor_ShouldSetFieldsCorrectly() {
        FileValidationError error = new FileValidationError(
                5,
                "ISBN",
                "ISBN is ongeldig");

        assertEquals(5, error.getRow());
        assertEquals("ISBN", error.getField());
        assertEquals("ISBN is ongeldig", error.getMessage());
    }

    @Test
    void multipleErrors_ShouldBeIndependent() {
        FileValidationError error1 = new FileValidationError(1, "Title", "Leeg");
        FileValidationError error2 = new FileValidationError(2, "Author", "Leeg");

        assertNotEquals(error1.getRow(), error2.getRow());
        assertNotEquals(error1.getField(), error2.getField());
    }
}