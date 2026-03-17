package edu.ap.bib_backend.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationResultTest {

    @Test
    void constructor_ShouldSetFieldsCorrectly() {
        FileValidationError error = new FileValidationError(2, "ISBN", "Fout");

        FileValidationResult result = new FileValidationResult(
                10,
                8,
                2,
                List.of(error));

        assertEquals(10, result.getTotalRows());
        assertEquals(8, result.getValidRows());
        assertEquals(2, result.getInvalidRows());

        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals("ISBN", result.getErrors().get(0).getField());
    }

    @Test
    void result_ShouldHandleEmptyErrorsList() {
        FileValidationResult result = new FileValidationResult(
                5,
                5,
                0,
                List.of());

        assertTrue(result.getErrors().isEmpty());
        assertEquals(0, result.getInvalidRows());
        assertEquals(5, result.getValidRows());
    }

    @Test
    void result_ShouldHandleMultipleErrors() {
        FileValidationError error1 = new FileValidationError(1, "ISBN", "Fout");
        FileValidationError error2 = new FileValidationError(2, "Title", "Leeg");

        FileValidationResult result = new FileValidationResult(
                2,
                0,
                2,
                List.of(error1, error2));

        assertEquals(2, result.getErrors().size());
        assertEquals("ISBN", result.getErrors().get(0).getField());
        assertEquals("Title", result.getErrors().get(1).getField());
    }
}