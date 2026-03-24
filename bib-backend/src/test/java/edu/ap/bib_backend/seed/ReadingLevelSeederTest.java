package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class ReadingLevelSeederTest {

    private ReadingLevelRepository repository;
    private ReadingLevelSeeder seeder;

    @BeforeEach
    void setUp() {
        repository = mock(ReadingLevelRepository.class);
        seeder = new ReadingLevelSeeder();
    }

    @Test
    @DisplayName("Given empty reading level table, when seedReadingLevels runs, then save default levels")
    void givenEmptyReadingLevelTable_whenSeedReadingLevelsRuns_thenSaveDefaultLevels() throws Exception {
        // Given
        given(repository.count()).willReturn(0L);

        CommandLineRunner runner = seeder.seedReadingLevels(repository);

        // When
        runner.run();

        // Then
        ArgumentCaptor<ReadingLevel> captor = ArgumentCaptor.forClass(ReadingLevel.class);
        then(repository).should(org.mockito.Mockito.times(4)).save(captor.capture());

        List<ReadingLevel> savedLevels = captor.getAllValues();

        assertThat(savedLevels).hasSize(4);
        assertThat(savedLevels)
                .extracting(ReadingLevel::getNiveauName)
                .containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("Given non-empty reading level table, when seedReadingLevels runs, then do not save levels")
    void givenNonEmptyReadingLevelTable_whenSeedReadingLevelsRuns_thenDoNotSaveLevels() throws Exception {
        // Given
        given(repository.count()).willReturn(2L);

        CommandLineRunner runner = seeder.seedReadingLevels(repository);

        // When
        runner.run();

        // Then
        then(repository).should(never()).save(org.mockito.ArgumentMatchers.any(ReadingLevel.class));
    }
}