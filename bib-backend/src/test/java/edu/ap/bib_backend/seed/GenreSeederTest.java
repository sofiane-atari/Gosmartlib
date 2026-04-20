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
    @DisplayName("Given empty genre table, when initGenres runs, then save default genres")
    void givenEmptyGenreTable_wheninitGenressRuns_thenSaveAllDefaultGenres() throws Exception {
        given(repository.count()).willReturn(0L);

        CommandLineRunner runner = seeder.initGenres(repository);

        runner.run();

        then(repository).should().save(new Genre("Fictie algemeen", "#95A5A6",
                "Diverse verzonnen verhalen die niet onder één specifiek subgenre vallen."));
        then(repository).should().save(new Genre("Non-fictie algemeen", "#16A085",
                "Informatieve boeken gebaseerd op de werkelijkheid en feiten."));

        ArgumentCaptor<List<Genre>> captor = ArgumentCaptor.forClass(List.class);
        then(repository).should().saveAll(captor.capture());

        List<Genre> savedGenres = captor.getValue();

        assertThat(savedGenres).hasSize(22);
        assertThat(savedGenres)
                .extracting(Genre::getName)
                .containsExactly(
                        "Literaire roman",
                        "Spanning / thriller",
                        "Detective / misdaad",
                        "Fantasy",
                        "Sciencefiction",
                        "Dystopie",
                        "Historische roman",
                        "Romantiek",
                        "Coming-of-age",
                        "Avontuur",
                        "Oorlog & conflict",
                        "Horror",
                        "Humor",
                        "Graphic novel / strip",
                        "Poëzie",
                        "Biografie / autobiografie",
                        "Wetenschap & technologie",
                        "Filosofie",
                        "Maatschappij & politiek",
                        "Psychologie",
                        "Geschiedenis",
                        "Kunst & cultuur");
    }

    @Test
    @DisplayName("Given non-empty genre table, when initGenres runs, then do not save genres")
    void givenNonEmptyGenreTable_wheninitGenressRuns_thenDoNotSaveGenres() throws Exception {
        // Given
        given(repository.count()).willReturn(5L);

        CommandLineRunner runner = seeder.initGenres(repository);

        // When
        runner.run();

        // Then
        then(repository).should(never()).saveAll(anyList());
    }
}