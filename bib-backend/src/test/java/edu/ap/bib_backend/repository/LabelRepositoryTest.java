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
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
import edu.ap.bib_backend.projection.LabelWithCount;

@DataJpaTest
@ActiveProfiles("test")
class LabelRepositoryTest {

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Helper to create test data
    private Label createLabel(String name, String color, String description) {
        Label label = new Label();
        label.setName(name);
        label.setColor(color);
        label.setDescription(description);
        return entityManager.persistAndFlush(label);
    }

    // ── findAllIncludeBookCount ──────────────────────────────────────────

    @Test
    void findAllIncludeBookCount_shouldReturnLabelsWithCorrectCount() {

        // Arrange
        Label label = createLabel("Fiction", "#FF0000", "Fiction books");

        Book book = new Book();
        book.setTitle("Test Book");

        // Add to both sides of the relationship (owning side is Book.labels)
        book.getLabels().add(label);
        label.getBooks().add(book);

        entityManager.persistAndFlush(book);
        entityManager.persistAndFlush(label);

        // Act
        Page<LabelWithCount> result = labelRepository.findAllIncludeBookCount(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Fiction");
        assertThat(result.getContent().get(0).getBookCount()).isEqualTo(1);
    }

    @Test
    void findAllIncludeBookCount_shouldReturnZeroCountWhenNoBooks() {

        // Arrange
        createLabel("Empty", "#0000FF", "No books here");

        // Act
        Page<LabelWithCount> result = labelRepository.findAllIncludeBookCount(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent().get(0).getBookCount()).isEqualTo(0);
    }

    // ── findAllFiltered ─────────────────────────────────────────────────

    @Test
    void findAllFiltered_shouldMatchByName() {

        // Arrange
        createLabel("Science", "#00FF00", "Science stuff");
        createLabel("Fiction", "#FF0000", "Fiction stuff");

        // Act
        Page<LabelWithCount> result = labelRepository.findAllFiltered("Sci", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Science");
    }

    @Test
    void findAllFiltered_shouldMatchByDescription() {

        // Arrange
        createLabel("Label1", "#0000FF", "Unique description");
        createLabel("Label2", "#FF0000", "Other text");

        // Act
        Page<LabelWithCount> result = labelRepository.findAllFiltered("Unique", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllFiltered_shouldFilterByBookCount() {

        // Arrange
        createLabel("LabelWithBooks", "#FF0000", "has books");
        createLabel("EmptyLabel", "#0000FF", "no books");

        // Act
        Page<LabelWithCount> result = labelRepository.findAllFiltered("", 0, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .allMatch(l -> l.getBookCount() == 0);
    }

    @Test
    void findAllFiltered_shouldReturnEmpty_whenNoMatch() {

        // Arrange
        createLabel("Fiction", "#FF0000", "desc");

        // Act
        Page<LabelWithCount> result = labelRepository.findAllFiltered("NOMATCH", null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ── searchByNameOrDescriptionOrColor ────────────────────────────────

    @Test
    void search_shouldMatchByName() {

        // Arrange
        createLabel("Horror", "#000000", "Scary books");
        createLabel("Romance", "#FFC0CB", "Love stories");

        // Act
        Page<Label> result = labelRepository.searchByNamerOrDescriptionOrColor("Hor", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Horror");
    }

    @Test
    void search_shouldMatchByColor() {

        // Arrange
        createLabel("Label1", "#00008B", "some desc");
        createLabel("Label2", "#FF0000", "other desc");

        // Act
        Page<Label> result = labelRepository.searchByNamerOrDescriptionOrColor("#00008B", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_shouldMatchByDescription() {

        // Arrange
        createLabel("Label1", "#FF0000", "very unique desc");

        // Act
        Page<Label> result = labelRepository.searchByNamerOrDescriptionOrColor("unique", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_shouldReturnAllOnEmptyString() {

        // Arrange
        createLabel("A", "#FF0000", "desc");
        createLabel("B", "#0000FF", "desc");

        // Act
        Page<Label> result = labelRepository.searchByNamerOrDescriptionOrColor("", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }
}