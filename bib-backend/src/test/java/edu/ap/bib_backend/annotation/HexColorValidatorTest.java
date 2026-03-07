package edu.ap.bib_backend.annotation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HexColorValidator}.
 * Pattern : Arrange – Act – Assert (AAA), one test per case.
 */
@DisplayName("HexColorValidator — Unit Tests")
class HexColorValidatorTest {

    private HexColorValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new HexColorValidator();
        validator.initialize(null);
        context  = mock(ConstraintValidatorContext.class);
    }

    // =========================================================================
    // VALID — 6-digit
    // =========================================================================

    @Test
    @DisplayName("6-digit uppercase white (#FFFFFF) → valid")
    void hexColor_uppercase6Digit_white_isValid() {
        // Arrange
        String color = "#FFFFFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit uppercase black (#000000) → valid")
    void hexColor_uppercase6Digit_black_isValid() {
        // Arrange
        String color = "#000000";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit lowercase (#ffffff) → valid")
    void hexColor_lowercase6Digit_isValid() {
        // Arrange
        String color = "#ffffff";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit mixed case (#1A2B3C) → valid")
    void hexColor_mixedCase6Digit_isValid() {
        // Arrange
        String color = "#1A2B3C";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit all-alpha lowercase (#abcdef) → valid")
    void hexColor_allAlphaLowercase6Digit_isValid() {
        // Arrange
        String color = "#abcdef";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit all-alpha uppercase (#ABCDEF) → valid")
    void hexColor_allAlphaUppercase6Digit_isValid() {
        // Arrange
        String color = "#ABCDEF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6-digit numeric-heavy (#123456) → valid")
    void hexColor_numericHeavy6Digit_isValid() {
        // Arrange
        String color = "#123456";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    // =========================================================================
    // VALID — 3-digit shorthand
    // =========================================================================

    @Test
    @DisplayName("3-digit uppercase white (#FFF) → valid")
    void hexColor_uppercase3Digit_white_isValid() {
        // Arrange
        String color = "#FFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3-digit uppercase black (#000) → valid")
    void hexColor_uppercase3Digit_black_isValid() {
        // Arrange
        String color = "#000";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3-digit lowercase (#fff) → valid")
    void hexColor_lowercase3Digit_isValid() {
        // Arrange
        String color = "#fff";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3-digit mixed case (#A1B) → valid")
    void hexColor_mixedCase3Digit_isValid() {
        // Arrange
        String color = "#A1B";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isTrue();
    }

    // =========================================================================
    // INVALID — null / empty
    // =========================================================================

    @Test
    @DisplayName("null → invalid")
    void hexColor_null_isInvalid() {
        // Arrange
        String color = null;

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("empty string → invalid")
    void hexColor_emptyString_isInvalid() {
        // Arrange
        String color = "";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hash only (#) → invalid")
    void hexColor_hashOnly_isInvalid() {
        // Arrange
        String color = "#";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    // =========================================================================
    // INVALID — missing hash prefix
    // =========================================================================

    @Test
    @DisplayName("6-digit without # prefix (FFFFFF) → invalid")
    void hexColor_missingHash_6Digit_isInvalid() {
        // Arrange
        String color = "FFFFFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("double hash prefix (##FFFFFF) → invalid")
    void hexColor_doubleHash_isInvalid() {
        // Arrange
        String color = "##FFFFFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    // =========================================================================
    // INVALID — wrong length
    // =========================================================================

    @Test
    @DisplayName("2-digit (#12) → invalid")
    void hexColor_2Digit_isInvalid() {
        // Arrange
        String color = "#12";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("4-digit (#FFFF) → invalid")
    void hexColor_4Digit_isInvalid() {
        // Arrange
        String color = "#FFFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("5-digit (#12345) → invalid")
    void hexColor_5Digit_isInvalid() {
        // Arrange
        String color = "#12345";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("7-digit (#1234567) → invalid")
    void hexColor_7Digit_isInvalid() {
        // Arrange
        String color = "#1234567";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    // =========================================================================
    // INVALID — illegal characters
    // =========================================================================

    @Test
    @DisplayName("invalid char G in 6-digit (#GGGGGG) → invalid")
    void hexColor_invalidCharG_6Digit_isInvalid() {
        // Arrange
        String color = "#GGGGGG";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("invalid chars in 3-digit (#xyz) → invalid")
    void hexColor_invalidChars_3Digit_isInvalid() {
        // Arrange
        String color = "#xyz";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    // =========================================================================
    // INVALID — whitespace
    // =========================================================================

    @Test
    @DisplayName("leading space before # ( #FFFFFF) → invalid")
    void hexColor_leadingSpace_isInvalid() {
        // Arrange
        String color = " #FFFFFF";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("trailing space after value (#FFFFFF ) → invalid")
    void hexColor_trailingSpace_isInvalid() {
        // Arrange
        String color = "#FFFFFF ";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    // =========================================================================
    // INVALID — wrong format entirely
    // =========================================================================

    @Test
    @DisplayName("CSS rgb() format → invalid")
    void hexColor_cssRgbFormat_isInvalid() {
        // Arrange
        String color = "rgb(0,0,0)";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("named color string (red) → invalid")
    void hexColor_namedColor_isInvalid() {
        // Arrange
        String color = "red";

        // Act
        boolean result = validator.isValid(color, context);

        // Assert
        assertThat(result).isFalse();
    }
}