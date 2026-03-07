package edu.ap.bib_backend.annotation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link HexColor} annotation.
 *
 * Uses the real Hibernate Validator (Bean Validation provider bundled with
 * spring-boot-starter-validation). No Spring context needed — the full
 * annotation-processing pipeline is exercised at minimal startup cost.
 *
 * Pattern : Arrange – Act – Assert (AAA), one test per case.
 */
@DisplayName("@HexColor annotation — Integration Tests")
class HexColorAnnotationIntegrationTest {

    // DTO that mirrors real-world usage of the annotation
    static class ColorRequest {
        @HexColor(message = "Invalid hex color")
        private final String color;

        ColorRequest(String color) {
            this.color = color;
        }
    }

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // =========================================================================
    // VALID — 6-digit
    // =========================================================================

    @Test
    @DisplayName("6-digit uppercase white (#FFFFFF) → no violations")
    void annotation_uppercase6Digit_white_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#FFFFFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("6-digit uppercase black (#000000) → no violations")
    void annotation_uppercase6Digit_black_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#000000");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("6-digit lowercase (#ffffff) → no violations")
    void annotation_lowercase6Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#ffffff");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("6-digit mixed case (#1A2B3C) → no violations")
    void annotation_mixedCase6Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#1A2B3C");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("6-digit all-alpha lowercase (#abcdef) → no violations")
    void annotation_allAlphaLowercase6Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#abcdef");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("6-digit numeric-heavy (#123456) → no violations")
    void annotation_numericHeavy6Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#123456");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    // =========================================================================
    // VALID — 3-digit shorthand
    // =========================================================================

    @Test
    @DisplayName("3-digit uppercase white (#FFF) → no violations")
    void annotation_uppercase3Digit_white_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#FFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("3-digit lowercase (#fff) → no violations")
    void annotation_lowercase3Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#fff");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("3-digit mixed case (#A1B) → no violations")
    void annotation_mixedCase3Digit_noViolations() {
        // Arrange
        ColorRequest request = new ColorRequest("#A1B");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    // =========================================================================
    // INVALID — null / empty — violation message + property path verified
    // =========================================================================

    @Test
    @DisplayName("null → one violation with correct message and property path")
    void annotation_null_oneViolationWithCorrectMessageAndPath() {
        // Arrange
        ColorRequest request = new ColorRequest(null);

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ColorRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Invalid hex color");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("color");
    }

    @Test
    @DisplayName("empty string → one violation with correct message and property path")
    void annotation_emptyString_oneViolationWithCorrectMessageAndPath() {
        // Arrange
        ColorRequest request = new ColorRequest("");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ColorRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Invalid hex color");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("color");
    }

    @Test
    @DisplayName("hash only (#) → one violation")
    void annotation_hashOnly_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    // =========================================================================
    // INVALID — missing / wrong hash prefix
    // =========================================================================

    @Test
    @DisplayName("missing # prefix (FFFFFF) → one violation")
    void annotation_missingHash_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("FFFFFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("double hash (##FFFFFF) → one violation")
    void annotation_doubleHash_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("##FFFFFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    // =========================================================================
    // INVALID — wrong length
    // =========================================================================

    @Test
    @DisplayName("2-digit (#12) → one violation")
    void annotation_2Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#12");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("4-digit (#FFFF) → one violation")
    void annotation_4Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#FFFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("5-digit (#12345) → one violation")
    void annotation_5Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#12345");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("7-digit (#1234567) → one violation")
    void annotation_7Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#1234567");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    // =========================================================================
    // INVALID — illegal characters
    // =========================================================================

    @Test
    @DisplayName("invalid char G in 6-digit (#GGGGGG) → one violation")
    void annotation_invalidCharG_6Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#GGGGGG");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("invalid chars in 3-digit (#xyz) → one violation")
    void annotation_invalidChars_3Digit_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#xyz");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    // =========================================================================
    // INVALID — whitespace
    // =========================================================================

    @Test
    @DisplayName("leading space ( #FFFFFF) → one violation")
    void annotation_leadingSpace_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest(" #FFFFFF");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("trailing space (#FFFFFF ) → one violation")
    void annotation_trailingSpace_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("#FFFFFF ");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    // =========================================================================
    // INVALID — wrong format entirely
    // =========================================================================

    @Test
    @DisplayName("CSS rgb() format → one violation")
    void annotation_cssRgbFormat_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("rgb(0,0,0)");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }

    @Test
    @DisplayName("named color string (red) → one violation")
    void annotation_namedColor_oneViolation() {
        // Arrange
        ColorRequest request = new ColorRequest("red");

        // Act
        Set<ConstraintViolation<ColorRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid hex color");
    }
}