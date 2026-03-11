package edu.ap.bib_backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceAlreadyExistsExceptionTest {

    // ==================== Constructor Tests ====================

    @Test
    void givenFieldNameAndValue_whenConstructorWithTwoArgs_thenExceptionCreatedWithDefaultMessage() {
        // Act
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("name", "Fiction");

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Resource bestaat al met name: 'Fiction'");
        assertThat(exception.getFieldName()).isEqualTo("name");
        assertThat(exception.getFieldValue()).isEqualTo("Fiction");
    }
}
