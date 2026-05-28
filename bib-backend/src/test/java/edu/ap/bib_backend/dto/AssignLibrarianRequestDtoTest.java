package edu.ap.bib_backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignLibrarianRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void Given_ValidUserId_When_Validated_Then_NoViolations() {
        // Arrange
        AssignLibrarianRequestDto dto = new AssignLibrarianRequestDto("user-1", null);

        // Act
        var violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void Given_BlankUserId_When_Validated_Then_HasViolation() {
        // Arrange
        AssignLibrarianRequestDto dto = new AssignLibrarianRequestDto(" ", null);

        // Act
        var violations = validator.validate(dto);

        // Assert
        assertEquals(1, violations.size());
    }
}
