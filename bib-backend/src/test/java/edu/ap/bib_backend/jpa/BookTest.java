package edu.ap.bib_backend.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.Book;

public class BookTest {

    @Test
    void constructor_setsAllFieldsCorrectly() {
        // Arrange
        String isbn = "123456789";
        String title = "Test Book";
        String author = "Test Author";
        int pages = 300;
        String imageUrl = "https://example.com/image.jpg";
        String description = "This is a test book";
        int releaseYear = 2023;

        // Act
        Book book = new Book(isbn, title, author, pages, imageUrl, description, releaseYear);

        // Assert
        assertThat(book.getIsbn()).isEqualTo(isbn);
        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthor()).isEqualTo(author);
        assertThat(book.getPages()).isEqualTo(pages);
        assertThat(book.getImageUrl()).isEqualTo(imageUrl);
        assertThat(book.getDescription()).isEqualTo(description);
        assertThat(book.getReleaseYear()).isEqualTo(releaseYear);
        assertThat(book.getReviewScore()).isNull();
    }
}
