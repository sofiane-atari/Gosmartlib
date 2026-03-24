package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class GenreSeederTest {

    private GenreRepository repository;
    private GenreSeeder seeder;

    @BeforeEach
    void setUp() {
        repository = mock(GenreRepository.class);
        seeder = new GenreSeeder();
    }

    @Test
    @DisplayName("Given empty genre table, when seedGenres runs, then save all default genres")
    void givenEmptyGenreTable_whenSeedGenresRuns_thenSaveAllDefaultGenres() throws Exception {
        // Given
        given(repository.count()).willReturn(0L);

        CommandLineRunner runner = seeder.seedGenres(repository);

        // When
        runner.run();

        // Then
        ArgumentCaptor<List<Genre>> captor = ArgumentCaptor.forClass(List.class);
        then(repository).should().saveAll(captor.capture());

        List<Genre> savedGenres = captor.getValue();
        assertThat(savedGenres).hasSize(15);
        assertThat(savedGenres)
                .extracting(Genre::getName)
                .containsExactly(
                        "Fantasy",
                        "Science Fiction",
                        "Thriller",
                        "Detective",
                        "Romantiek",
                        "Historisch",
                        "Horror",
                        "Avontuur",
                        "Biografie",
                        "Jeugd",
                        "Poëzie",
                        "Schoolpublicatie",
                        "Tijdschrift",
                        "Stripverhaal",
                        "Educatief");
    }

    @Test
    @DisplayName("Given non-empty genre table, when seedGenres runs, then do not save genres")
    void givenNonEmptyGenreTable_whenSeedGenresRuns_thenDoNotSaveGenres() throws Exception {
        // Given
        given(repository.count()).willReturn(5L);

        CommandLineRunner runner = seeder.seedGenres(repository);

        // When
        runner.run();

        // Then
        then(repository).should(never()).saveAll(anyList());
    }
}