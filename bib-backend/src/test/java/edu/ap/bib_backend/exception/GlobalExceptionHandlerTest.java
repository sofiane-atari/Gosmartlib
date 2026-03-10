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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
                "Label met de naam 'Fiction' bestaat al.");

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
    void givenRuntimeException_whenHandleRuntime_thenReturn500InternalServerError() {
        // Arrange
        RuntimeException exception = new RuntimeException("Database connection failed");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntime(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Database connection failed");
    }

    @Test
    void givenRuntimeExceptionWithNullMessage_whenHandleRuntime_thenReturnNullMessage() {
        // Arrange
        RuntimeException exception = new RuntimeException();

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntime(exception);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getMessage()).isNull();
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
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Validation failed");
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
}
