package edu.ap.bib_backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    // ==================== Test Controller for MethodParameter creation
    // ====================

    @RestController
    static class TestController {
        @GetMapping("/test")
        public String test() {
            return "success";
        }
    }

    // ==================== ResourceAlreadyExistsException Tests
    // ====================

    @Test
    void givenResourceAlreadyExistsException_whenHandleResourceAlreadyExists_thenReturn409Conflict() {
        // Arrange
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("name", "Fiction",
                "genre met de naam 'Fiction' bestaat al.");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceAlreadyExists(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Item bestaat al");
        assertThat(response.getBody().getError().getDetails()).hasSize(1);
        assertThat(response.getBody().getError().getDetails().get(0).getField()).isEqualTo("name");
    }

    @Test
    void givenResourceAlreadyExistsExceptionWithCustomMessage_whenHandleResourceAlreadyExists_thenReturnCustomMessage() {
        // Arrange
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("isbn", "123456789",
                "Book with this ISBN already exists");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceAlreadyExists(exception);

        // Assert
        assertThat(response.getBody().getError().getDetails().get(0).getField()).isEqualTo("isbn");
        // The message from the exception is stored in the ErrorDetail message
        assertThat(response.getBody().getError().getDetails().get(0).getMessage())
                .isEqualTo("Book with this ISBN already exists");
    }

    @Test
    void givenResourceAlreadyExistsExceptionWithDefaultMessage_whenHandleResourceAlreadyExists_thenReturnDefaultMessage() {
        // Arrange - Test constructor without custom message (uses default message)
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("name", "Fiction");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceAlreadyExists(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Item bestaat al");
        assertThat(response.getBody().getError().getDetails()).hasSize(1);
        assertThat(response.getBody().getError().getDetails().get(0).getField()).isEqualTo("name");
        assertThat(response.getBody().getError().getDetails().get(0).getMessage())
                .isEqualTo("Resource bestaat al met name: 'Fiction'");
    }

    // ==================== IllegalStateException Tests ====================

    @Test
    void givenIllegalStateException_whenHandleIllegalState_thenReturn400BadRequest() {
        // Arrange
        IllegalStateException exception = new IllegalStateException("Invalid state: entity already exists");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalState(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(400);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Invalid state: entity already exists");
    }

    @Test
    void givenEmptyIllegalStateException_whenHandleIllegalState_thenReturnEmptyMessage() {
        // Arrange
        IllegalStateException exception = new IllegalStateException("");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalState(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getMessage()).isEmpty();
    }

    // ==================== RuntimeException Tests ====================

    @Test
    void Given_RuntimeException_When_HandleRuntime_Then_ReturnGeneric500InternalServerError() {
        // Arrange
        RuntimeException exception = new RuntimeException("Database connection failed");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntime(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Er is een onverwachte fout opgetreden.");
        assertThat(response.getBody().getError().getMessage()).doesNotContain("Database connection failed");
    }

    @Test
    void Given_RuntimeExceptionWithNullMessage_When_HandleRuntime_Then_ReturnGeneric500InternalServerError() {
        // Arrange
        RuntimeException exception = new RuntimeException();

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntime(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Er is een onverwachte fout opgetreden.");
    }

    @Test
    void Given_ResponseStatusExceptionWithServerError_When_HandleResponseStatus_Then_ReturnGenericMessage() {
        // Arrange
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OneRoster data ophalen mislukt: secret-token-123");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatus(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Er is een onverwachte fout opgetreden.");
        assertThat(response.getBody().getError().getMessage()).doesNotContain("secret-token-123");
    }

    // ==================== MethodArgumentNotValidException Tests
    // ====================

    @Test
    void givenMethodArgumentNotValidException_whenHandleMethodArgumentNotValid_thenReturn400BadRequest()
            throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testDto");
        Method method = TestController.class.getMethod("test");
        MethodParameter methodParameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(400);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Validatie mislukt");
    }

    @Test
    void givenMethodArgumentNotValidExceptionWithFieldErrors_whenHandleMethodArgumentNotValid_thenReturnFieldErrors()
            throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testDto");
        bindingResult.addError(new FieldError("testDto", "name", "Name is required"));
        bindingResult.addError(new FieldError("testDto", "email", "Email must be valid"));

        Method method = TestController.class.getMethod("test");
        MethodParameter methodParameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getDetails()).hasSize(2);
    }

    @Test
    void givenMethodArgumentNotValidExceptionWithMultipleErrors_whenHandleMethodArgumentNotValid_thenReturnAllErrors()
            throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testDto");
        bindingResult.addError(new FieldError("testDto", "field1", "Error 1"));
        bindingResult.addError(new FieldError("testDto", "field2", "Error 2"));
        bindingResult.addError(new FieldError("testDto", "field3", "Error 3"));

        Method method = TestController.class.getMethod("test");
        MethodParameter methodParameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception);

        // Assert
        assertThat(response.getBody().getError().getDetails()).hasSize(3);
    }

    // ==================== ResourceNotFoundException Tests ====================

    @Test
    void givenResourceNotFoundException_whenHandleResourceNotFound_thenReturn404NotFound() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("name", "Fiction");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(404);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Item niet gevonden");
        assertThat(response.getBody().getError().getDetails()).hasSize(1);
        assertThat(response.getBody().getError().getDetails().get(0).getField()).isEqualTo("name");
    }

    @Test
    void givenResourceNotFoundExceptionWithCustomMessage_whenHandleResourceNotFound_thenReturnCustomMessage() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("id", "Book with id 999 not found");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError().getDetails().get(0).getMessage())
                .isEqualTo("Book with id 999 not found");
    }

    // ==================== DataIntegrityViolationException Tests
    // ====================

    @Test
    void givenDataIntegrityViolationExceptionWithForeignKey_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception(
                "Cannot delete or update a parent row: a foreign key constraint fails (`bib_db`.`book`, CONSTRAINT `fk_book_genre` FOREIGN KEY (`genre_name`) REFERENCES `genre` (`name`))");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Foreign key constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("nog gekoppeld");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithUniqueConstraint_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception("Duplicate entry 'Fiction' for key 'name'");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Unique constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("bestaat al");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithNullValue_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception("Column 'name' cannot be null");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Null constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("verplicht veld");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithTruncatedValue_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception("Data too long for column 'description' at row 1");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Truncated value", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("te lang");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithUnknownError_whenHandleDataIntegrityViolation_thenReturnGenericMessage() {
        // Arrange
        Exception rootCause = new Exception("Some unknown database error");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Unknown error", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("conflict");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithNoRootCause_whenHandleDataIntegrityViolation_thenReturnGenericMessage() {
        // Arrange
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Unknown error");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("conflict");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithFkUsingTableName_whenHandleDataIntegrityViolation_thenExtractTableName() {
        // Arrange
        Exception rootCause = new Exception(
                "Cannot delete or update a parent row: a foreign key constraint fails (`bib_db`.`book_genres`, CONSTRAINT `fk_genre` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`))");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Foreign key constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getDetails().get(0).getMessage()).doesNotContain("genres");
        assertThat(response.getBody().getError().getDetails().get(0).getMessage()).contains("gerelateerde items");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithDuplicateEmail_whenHandleDataIntegrityViolation_thenExtractFieldAndValue() {
        // Arrange
        Exception rootCause = new Exception("Duplicate entry 'test@example.com' for key 'users.email'");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Unique constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getDetails().get(0).getMessage()).doesNotContain("test@example.com");
        assertThat(response.getBody().getError().getDetails().get(0).getMessage()).doesNotContain("users.email");
        assertThat(response.getBody().getError().getDetails().get(0).getMessage()).contains("unieke waarde");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithFkUnderscore_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception("Cannot delete or update parent row: foreign key constraint fk_book_genre");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "FK constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("nog gekoppeld");
    }

    @Test
    void givenDataIntegrityViolationExceptionWithUniqueWithoutDuplicate_whenHandleDataIntegrityViolation_thenReturn409Conflict() {
        // Arrange
        Exception rootCause = new Exception("Unique constraint failed: value already exists");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Unique constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("bestaat al");
    }

    // ==================== Edge Cases ====================

    @Test
    void givenResourceAlreadyExistsExceptionWithNullFieldName_whenHandleResourceAlreadyExists_thenReturn409() {
        // Arrange
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException(null, "Fiction",
                "Genre already exists");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceAlreadyExists(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void givenIllegalStateExceptionWithNullMessage_whenHandleIllegalState_thenReturn400() {
        // Arrange
        IllegalStateException exception = new IllegalStateException((String) null);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalState(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        // Message can be null for IllegalStateException
        assertThat(response.getBody().getError().getMessage()).isNull();
    }

    @Test
    void givenMethodArgumentNotValidExceptionWithNoFieldErrors_whenHandleMethodArgumentNotValid_thenReturn400()
            throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testDto");
        // Don't add any errors

        Method method = TestController.class.getMethod("test");
        MethodParameter methodParameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getDetails()).isEmpty();
    }

    @Test
    void givenResourceNotFoundExceptionWithNullFieldName_whenHandleResourceNotFound_thenReturn404() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException(null, "Not found");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void givenDataIntegrityViolationExceptionWithNotNullRootCause_whenHandleDataIntegrityViolation_thenReturn409() {
        // Arrange
        Exception rootCause = new Exception("NOT NULL constraint failed");
        org.springframework.dao.DataIntegrityViolationException exception = new org.springframework.dao.DataIntegrityViolationException(
                "Constraint failed", rootCause);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).contains("verplicht veld");
    }
}
