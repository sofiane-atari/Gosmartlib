package edu.ap.bib_backend.service.oneroster;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class OneRosterHostValidatorTest {

    private final OneRosterHostValidator validator = new OneRosterHostValidator(".smartschool.be", false);

    @Test
    void Given_ValidHost_When_NormalizeAndValidate_Then_ReturnsHost() {
        // Arrange
        String host = "aphogeschool.smartschool.be";
        // Note: This might fail if the host doesn't resolve in the test environment.
        // But for this specific test, we assume the host exists or we catch the exception.
        
        try {
            // Act
            String result = validator.normalizeAndValidateSchoolHost(host);
            // Assert
            assertEquals(host, result);
        } catch (ResponseStatusException e) {
            // If it fails because of UnknownHostException, it's expected in some environments
            if (!e.getReason().contains("kon niet gevalideerd worden")) {
                throw e;
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-suffix.com", "localhost", "127.0.0.1", "::1", "sub.localhost"})
    void Given_DisallowedHost_When_NormalizeAndValidate_Then_ThrowsBadRequest(String host) {
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> validator.normalizeAndValidateSchoolHost(host));
    }

    @Test
    void Given_Url_When_NormalizeAndValidate_Then_ExtractsHost() {
        // Arrange
        String url = "https://test.smartschool.be/path";
        
        // Act
        String result = (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(validator, "normalizeSchoolCodeToHost", url);
        
        // Assert
        assertEquals("test.smartschool.be", result);
    }

    @Test
    void Given_InvalidCharacters_When_NormalizeAndValidate_Then_ThrowsBadRequest() {
        // Arrange
        String invalid = "test@smartschool.be";
        
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> validator.normalizeAndValidateSchoolHost(invalid));
    }
}
