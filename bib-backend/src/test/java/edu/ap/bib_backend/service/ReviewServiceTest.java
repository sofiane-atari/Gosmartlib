package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Review;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookRepository bookRepository;
    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("Haal reviews op - Succes")
    void whenGetReviewsForBook_givenValidBookId_thenReturnReviewDTOList() {
        // Arrange
        Long bookId = 1L;
        Book mockBook = new Book(); 
        mockBook.setId(bookId);

        
        Review review1 = new Review("Super boek", 5, mockBook);
        review1.setId(10L);
        Review review2 = new Review("Minder goed", 2, mockBook);
        review2.setId(11L);

        List<Review> mockDatabaseList = Arrays.asList(review1, review2);
        Mockito.when(reviewRepository.findByBookId(bookId)).thenReturn(mockDatabaseList);

        // Act
        List<ReviewDTO> result = reviewService.getReviewsForBook(bookId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Super boek", result.get(0).getComment());
        assertEquals(5, result.get(0).getRating());
        
        Mockito.verify(reviewRepository).findByBookId(bookId);
    }

    @Test
    @DisplayName("Voeg review toe - Succes")
    void whenAddReviewToBook_givenValidBookIdAndDTO_thenReturnSavedReviewDTO() {
        // Arrange
        Long bookId = 1L;
        ReviewDTO inputDto = new ReviewDTO(null, "Echt een aanrader", 4);
        
        Book mockBook = new Book();
        mockBook.setId(bookId);

        Review savedEntity = new Review(inputDto.getComment(), inputDto.getRating(), mockBook);
        savedEntity.setId(99L);

        Mockito.when(bookRepository.findById(bookId)).thenReturn(Optional.of(mockBook));
        Mockito.when(reviewRepository.save(any(Review.class))).thenReturn(savedEntity);

        // Act
        ReviewDTO result = reviewService.addReviewToBook(bookId, inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("Echt een aanrader", result.getComment());
        assertEquals(4, result.getRating());

        
        Mockito.verify(bookRepository).findById(bookId);
        Mockito.verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Voeg review toe aan onbekend boek - Gooit Exception")
    void whenAddReviewToBook_givenInvalidBookId_thenThrowRuntimeException() {
        // Arrange
        Long invalidBookId = 999L;
        ReviewDTO inputDto = new ReviewDTO(null, "Dit boek bestaat niet", 1);
        Mockito.when(bookRepository.findById(invalidBookId)).thenReturn(Optional.empty());

        // Act & Assert (Omdat het een exception is, testen we Act en Assert tegelijk)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.addReviewToBook(invalidBookId, inputDto);
        });

        assertEquals("Boek niet gevonden met id: 999", exception.getMessage());
        
        
        Mockito.verify(reviewRepository, Mockito.never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Haal gemiddelde rating op - Succes")
    void whenGetAverageRating_givenValidBookId_thenReturnAverageValue() {
        // Arrange
        Long bookId = 1L;
        Double expectedAverage = 4.5;

        Mockito.when(reviewRepository.getAverageRatingForBook(bookId)).thenReturn(expectedAverage);

        // Act
        Double result = reviewService.getAverageRating(bookId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedAverage, result);
        Mockito.verify(reviewRepository).getAverageRatingForBook(bookId);
    }
}