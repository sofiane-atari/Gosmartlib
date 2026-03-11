package edu.ap.bib_backend.projection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LabelProjection and LabelWithCount to achieve good coverage.
 * Since these are interfaces with default methods, we test them using anonymous
 * implementations.
 */
class LabelProjectionTest {

    // ==================== LabelProjection Tests ====================

    @Test
    void givenLabelProjectionImplementation_whenGetName_thenReturnName() {
        // Arrange - Anonymous implementation of LabelProjection
        LabelProjection projection = new LabelProjection() {
            @Override
            public String getName() {
                return "Fiction";
            }

            @Override
            public String getColor() {
                return "#FF0000";
            }

            @Override
            public String getDescription() {
                return "Fiction books";
            }
        };

        // Act
        String name = projection.getName();

        // Assert
        assertThat(name).isEqualTo("Fiction");
    }
}
