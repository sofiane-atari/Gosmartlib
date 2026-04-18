package edu.ap.bib_backend.projection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenreProjectionTest {


    @Test
    void givenGenreProjectionImplementation_whenGetName_thenReturnName() {
        // Arrange - Anonymous implementation of GenreProjection
        GenreProjection projection = new GenreProjection() {

            @Override
            public Long getId() {
                return null;
            }
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
