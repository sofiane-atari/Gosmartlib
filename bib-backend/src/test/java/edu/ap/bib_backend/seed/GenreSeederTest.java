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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class GenreSeederTest {

    private GenreRepository repository;
    private GenreSeeder seeder;

    @BeforeEach
    void setUp() {
        repository = mock(GenreRepository.class);
        seeder = new GenreSeeder();
    }

    @SuppressWarnings("rawtypes")
@Test
    @DisplayName("Given empty genre table, when initGenres runs, then save hoofdgenres and subgenres")
    void givenEmptyGenreTable_whenInitGenresRuns_thenSaveHoofdgenresAndSubgenres() throws Exception {
        given(repository.count()).willReturn(0L);

        CommandLineRunner runner = seeder.initGenres(repository);

        runner.run();

        ArgumentCaptor<Genre> savedGenreCaptor = ArgumentCaptor.forClass(Genre.class);
        then(repository).should(times(2)).save(savedGenreCaptor.capture());

        List<Genre> savedMainGenres = savedGenreCaptor.getAllValues();
        assertThat(savedMainGenres).hasSize(2);

        Genre fictie = savedMainGenres.get(0);
        Genre nonFictie = savedMainGenres.get(1);

        assertThat(fictie.getName()).isEqualTo("Fictie algemeen");
        assertThat(fictie.getColor()).isEqualTo("#95A5A6");
        assertThat(fictie.getDescription())
                .isEqualTo("Diverse verzonnen verhalen die niet onder één specifiek subgenre vallen.");
        assertThat(fictie.getParentGenre()).isNull();

        assertThat(nonFictie.getName()).isEqualTo("Non-fictie algemeen");
        assertThat(nonFictie.getColor()).isEqualTo("#16A085");
        assertThat(nonFictie.getDescription())
                .isEqualTo("Informatieve boeken gebaseerd op de werkelijkheid en feiten.");
        assertThat(nonFictie.getParentGenre()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Genre>> subGenreListCaptor = ArgumentCaptor.forClass((Class) List.class);
        then(repository).should().saveAll(subGenreListCaptor.capture());

        List<Genre> savedSubGenres = subGenreListCaptor.getValue();

        assertThat(savedSubGenres).hasSize(22);
        assertThat(savedSubGenres)
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
                        "Kunst & cultuur"
                );

        assertThat(savedSubGenres.subList(0, 15))
                .allSatisfy(subGenre -> assertThat(subGenre.getParentGenre()).isSameAs(fictie));

        assertThat(savedSubGenres.subList(15, 22))
                .allSatisfy(subGenre -> assertThat(subGenre.getParentGenre()).isSameAs(nonFictie));
    }

    @Test
    @DisplayName("Given non-empty genre table, when initGenres runs, then do not save anything")
    void givenNonEmptyGenreTable_whenInitGenresRuns_thenDoNotSaveAnything() throws Exception {
        given(repository.count()).willReturn(5L);

        CommandLineRunner runner = seeder.initGenres(repository);

        runner.run();

        then(repository).should(never()).save(any(Genre.class));
        then(repository).should(never()).saveAll(anyList());
    }
}