package edu.ap.bib_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.projection.GenreWithCount;

@DataJpaTest
@ActiveProfiles("test")
class GenreRepositoryTest {

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Helper to create test data
    private Genre createGenre(String name, String color, String description) {
        Genre genre = new Genre();
        genre.setName(name);
        genre.setColor(color);
        genre.setDescription(description);
        return entityManager.persistAndFlush(genre);
    }

    // ── findAllIncludeBookCount ──────────────────────────────────────────

    @Test
    void findAllIncludeBookCount_shouldReturnGenresWithCorrectCount() {

        // Arrange
        Genre genre = createGenre("Fiction", "#FF0000", "Fiction books");

        Book book = new Book();
        book.setTitle("Test Book");

        // Add to both sides of the relationship (owning side is Book.genres)
        book.getGenres().add(genre);
        genre.getBooks().add(book);

        entityManager.persistAndFlush(book);
        entityManager.persistAndFlush(genre);

        // Act
        Page<GenreWithCount> result = genreRepository.findAllIncludeBookCount(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Fiction");
        assertThat(result.getContent().get(0).getBookCount()).isEqualTo(1);
    }

    @Test
    void findAllIncludeBookCount_shouldReturnZeroCountWhenNoBooks() {

        // Arrange
        createGenre("Empty", "#0000FF", "No books here");

        // Act
        Page<GenreWithCount> result = genreRepository.findAllIncludeBookCount(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent().get(0).getBookCount()).isEqualTo(0);
    }

    // ── findAllFiltered ─────────────────────────────────────────────────

    @Test
    void findAllFiltered_shouldMatchByName() {

        // Arrange
        createGenre("Science", "#00FF00", "Science stuff");
        createGenre("Fiction", "#FF0000", "Fiction stuff");

        // Act
        Page<GenreWithCount> result = genreRepository.findAllFiltered("Sci", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Science");
    }

    @Test
    void findAllFiltered_shouldMatchByDescription() {

        // Arrange
        createGenre("Genre1", "#0000FF", "Unique description");
        createGenre("Genre2", "#FF0000", "Other text");

        // Act
        Page<GenreWithCount> result = genreRepository.findAllFiltered("Unique", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllFiltered_shouldFilterByBookCount() {

        // Arrange
        createGenre("GenreWithBooks", "#FF0000", "has books");
        createGenre("EmptyGenre", "#0000FF", "no books");

        // Act
        Page<GenreWithCount> result = genreRepository.findAllFiltered("", 0, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .allMatch(g -> g.getBookCount() == 0);
    }

    @Test
    void findAllFiltered_shouldReturnEmpty_whenNoMatch() {

        // Arrange
        createGenre("Fiction", "#FF0000", "desc");

        // Act
        Page<GenreWithCount> result = genreRepository.findAllFiltered("NOMATCH", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ── searchByNameOrDescriptionOrColor ────────────────────────────────

    @Test
    void searchByNamerOrDescriptionOrColor_shouldMatchByName() {

        // Arrange
        createGenre("Horror", "#000000", "Scary books");
        createGenre("Romance", "#FFC0CB", "Love stories");

        // Act
        Page<Genre> result = genreRepository.searchByNameOrDescriptionOrColor("Hor", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Horror");
    }

    @Test
    void searchByNamerOrDescriptionOrColor_shouldMatchByColor() {

        // Arrange
        createGenre("Genre1", "#00008B", "some desc");
        createGenre("Genre2", "#FF0000", "other desc");

        // Act
        Page<Genre> result = genreRepository.searchByNameOrDescriptionOrColor("#00008B", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchByNamerOrDescriptionOrColor_shouldMatchByDescription() {

        // Arrange
        createGenre("Genre1", "#FF0000", "very unique desc");

        // Act
        Page<Genre> result = genreRepository.searchByNameOrDescriptionOrColor("unique", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchByNamerOrDescriptionOrColor_shouldReturnAllOnEmptyString() {

        // Arrange
        createGenre("A", "#FF0000", "desc");
        createGenre("B", "#0000FF", "desc");

        // Act
        Page<Genre> result = genreRepository.searchByNameOrDescriptionOrColor("", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }
}
