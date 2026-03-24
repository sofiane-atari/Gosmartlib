package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.review.Review;
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
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private ProfanityFilterService profanityFilterService;
    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("Get reviews - Success")
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
    @DisplayName("Get all reviews - Success")
    void whenGetAllReviews_thenReturnReviewDTOList() {
        // Arrange
        Book mockBook = new Book();
        mockBook.setId(1L);

        Review review1 = new Review("Super boek", 5, mockBook);
        review1.setId(10L);
        Review review2 = new Review("Minder goed", 2, mockBook);
        review2.setId(11L);

        List<Review> mockDatabaseList = Arrays.asList(review1, review2);
        Mockito.when(reviewRepository.findAll()).thenReturn(mockDatabaseList);

        // Act
        List<ReviewDTO> result = reviewService.getAllReviews();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());

        Mockito.verify(reviewRepository).findAll();
    }

    @Test
    @DisplayName("Add review - Success (No profanity)")
    void whenAddReviewToBook_givenValidBookIdAndCleanDTO_thenReturnSavedReviewDTO() {
        // Arrange
        Long bookId = 1L;
        ReviewDTO inputDto = new ReviewDTO(null, "Highly recommended", 4, 0, null, null, null);

        Book mockBook = new Book();
        mockBook.setId(bookId);

        Review savedEntity = new Review(inputDto.getComment(), inputDto.getRating(), mockBook);
        savedEntity.setId(99L);

        // We vertellen de mock dat deze tekst GEEN scheldwoorden bevat
        Mockito.when(profanityFilterService.containsProfanity(inputDto.getComment())).thenReturn(false);
        Mockito.when(bookRepository.findById(bookId)).thenReturn(Optional.of(mockBook));
        Mockito.when(reviewRepository.save(any(Review.class))).thenReturn(savedEntity);

        // Act
        ReviewDTO result = reviewService.addReviewToBook(bookId, inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("Highly recommended", result.getComment());
        assertEquals(4, result.getRating());

        Mockito.verify(profanityFilterService).containsProfanity(inputDto.getComment());
        Mockito.verify(bookRepository).findById(bookId);
        Mockito.verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Add review - Profanity detected (Throws Exception)")
    void whenAddReviewToBook_givenProfanity_thenThrowRuntimeException() {
        // Arrange
        Long bookId = 1L;
        ReviewDTO inputDto = new ReviewDTO(null, "This book is really shit", 1, 0, null, null, null);
        Mockito.when(profanityFilterService.containsProfanity(inputDto.getComment())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.addReviewToBook(bookId, inputDto);
        });
        assertEquals("Review contains profanity", exception.getMessage());

        Mockito.verify(bookRepository, Mockito.never()).findById(any());
        Mockito.verify(reviewRepository, Mockito.never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Add review to unknown book - Throws Exception")
    void whenAddReviewToBook_givenInvalidBookId_thenThrowRuntimeException() {
        // Arrange
        Long invalidBookId = 999L;
        ReviewDTO inputDto = new ReviewDTO(null, "This book does not exist", 1, 0, null, null, null);

        Mockito.when(profanityFilterService.containsProfanity(anyString())).thenReturn(false);
        Mockito.when(bookRepository.findById(invalidBookId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.addReviewToBook(invalidBookId, inputDto);
        });

        assertEquals("Didn't find book with id: 999", exception.getMessage());
        Mockito.verify(reviewRepository, Mockito.never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Get average rating - Success")
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

    @Test
    @DisplayName("Delete review - Succes")
    void whenDeleteReview_givenExistingId_thenDeleteReview() {
        // Arrange
        Long reviewId = 1L;
        Review review = new Review("Test review", 5, new Book());
        review.setId(reviewId);

        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // Act
        reviewService.deleteReview(reviewId);

        // Assert
        Mockito.verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("Verwijder review - Niet gevonden gooit Exception")
    void whenDeleteReview_givenInvalidId_thenThrowException() {
        // Arrange
        Long reviewId = 999L;

        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.deleteReview(reviewId);
        });

        assertEquals("Didn't find review with id: 999", exception.getMessage());
        Mockito.verify(reviewRepository, Mockito.never()).delete(any());
    }

    @Test
    @DisplayName("Get reviews - No reviews found returns empty list")
    void whenGetReviewsForBook_givenBookWithoutReviews_thenReturnEmptyList() {
        // Arrange
        Long bookId = 2L;

        Mockito.when(reviewRepository.findByBookId(bookId)).thenReturn(List.of());

        // Act
        List<ReviewDTO> result = reviewService.getReviewsForBook(bookId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        Mockito.verify(reviewRepository).findByBookId(bookId);
    }

    @Test
    @DisplayName("Get average rating - No reviews returns 0.0")
    void whenGetAverageRating_givenBookWithoutReviews_thenReturnZero() {
        // Arrange
        Long bookId = 3L;

        Mockito.when(reviewRepository.getAverageRatingForBook(bookId)).thenReturn(0.0);

        // Act
        Double result = reviewService.getAverageRating(bookId);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result);
        Mockito.verify(reviewRepository).getAverageRatingForBook(bookId);
    }

    @Test
    @DisplayName("Report review - Existing review increments report count")
    void whenReportReview_givenExistingReview_thenIncrementReportCount() {
        // Arrange
        Long reviewId = 1L;

        Book book = new Book();
        book.setId(42L);

        Review review = new Review();
        review.setId(reviewId);
        review.setBook(book);
        review.setComment("Good book");
        review.setRating(4);
        review.setReportCount(2);

        Mockito.when(reviewRepository.findById(reviewId))
                .thenReturn(Optional.of(review));

        Mockito.when(reviewRepository.save(Mockito.any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewDTO result = reviewService.reportReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(reviewId, result.getId());
        assertEquals("Good book", result.getComment());
        assertEquals(4, result.getRating());
        assertEquals(3, result.getReportCount()); // incremented

        Mockito.verify(reviewRepository).findById(reviewId);
        Mockito.verify(reviewRepository).save(review);
    }
}