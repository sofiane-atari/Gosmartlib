package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.entity.review.ReviewReport;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfanityFilterService profanityFilterService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("givenValidBookId whenGetReviewsForBook thenReturnReviewDtoList")
    void givenValidBookId_whenGetReviewsForBook_thenReturnReviewDtoList() {
        // Arrange
        Long bookId = 1L;

        Book mockBook = new Book();
        mockBook.setId(bookId);
        mockBook.setTitle("Test Title");
        mockBook.setAuthor("Test Author");

        User user = new User();
        user.setId("user-1");

        Review review1 = new Review("Super boek", 5, mockBook, user);
        review1.setId(10L);
        review1.setModerationStatus(ReviewModerationStatus.ACTIVE);

        Review review2 = new Review("Minder goed", 2, mockBook, user);
        review2.setId(11L);
        review2.setModerationStatus(ReviewModerationStatus.ACTIVE);

        List<Review> mockDatabaseList = Arrays.asList(review1, review2);

        Mockito.when(authentication.getName()).thenReturn("user-1");
        Mockito.when(reviewRepository.findByBookId(bookId)).thenReturn(mockDatabaseList);
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(10L, "user-1")).thenReturn(false);
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(11L, "user-1")).thenReturn(false);

        // Act
        List<ReviewDTO> result = reviewService.getReviewsForBook(bookId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Super boek", result.get(0).getComment());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Test Title", result.get(0).getBookTitle());
        assertEquals("Test Author", result.get(0).getBookAuthor());

        Mockito.verify(reviewRepository).findByBookId(bookId);
    }

    @Test
    @DisplayName("whenGetAllReviews thenReturnReviewDtoList")
    void whenGetAllReviews_thenReturnReviewDtoList() {
        // Arrange
        Book mockBook = new Book();
        mockBook.setId(1L);
        mockBook.setTitle("Titel");
        mockBook.setAuthor("Auteur");

        User reviewOwner = new User();
        reviewOwner.setId("student-1");

        Review review1 = new Review("Super boek", 5, mockBook, reviewOwner);
        review1.setId(10L);
        review1.setModerationStatus(ReviewModerationStatus.ACTIVE);

        Review review2 = new Review("Minder goed", 2, mockBook, reviewOwner);
        review2.setId(11L);
        review2.setModerationStatus(ReviewModerationStatus.FLAGGED);

        List<Review> mockDatabaseList = Arrays.asList(review1, review2);

        Mockito.when(authentication.getName()).thenReturn("user-1");
        Mockito.when(reviewRepository.findAll()).thenReturn(mockDatabaseList);
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(10L, "user-1")).thenReturn(false);
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(11L, "user-1")).thenReturn(true);

        // Act
        List<ReviewDTO> result = reviewService.getAllReviews(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(ReviewModerationStatus.FLAGGED, result.get(1).getModerationStatus());
        assertTrue(result.get(1).isReportedByCurrentUser());

        Mockito.verify(reviewRepository).findAll();
    }

    @Test
    @DisplayName("givenValidBookIdAndCleanReviewDto whenAddReviewToBook thenReturnSavedReviewDto")
    void givenValidBookIdAndCleanReviewDto_whenAddReviewToBook_thenReturnSavedReviewDto() {
        // Arrange
        Long bookId = 1L;

        ReviewDTO inputDto = new ReviewDTO(
                null,
                "Highly recommended",
                4,
                0,
                null,
                null,
                null,
                null,
                false
        );

        Book mockBook = new Book();
        mockBook.setId(bookId);
        mockBook.setTitle("Test Title");
        mockBook.setAuthor("Test Author");

        User reviewOwner = new User();
        reviewOwner.setId("user-1");

        Review savedEntity = new Review(inputDto.getComment(), inputDto.getRating(), mockBook, reviewOwner);
        savedEntity.setId(99L);
        savedEntity.setModerationStatus(ReviewModerationStatus.ACTIVE);

        Mockito.when(authentication.getName()).thenReturn("user-1");
        Mockito.when(profanityFilterService.containsProfanity(inputDto.getComment())).thenReturn(false);
        Mockito.when(bookRepository.findById(bookId)).thenReturn(Optional.of(mockBook));
        Mockito.when(userRepository.findById("user-1")).thenReturn(Optional.of(reviewOwner));
        Mockito.when(reviewRepository.save(any(Review.class))).thenReturn(savedEntity);
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(99L, "user-1")).thenReturn(false);

        // Act
        ReviewDTO result = reviewService.addReviewToBook(bookId, inputDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("Highly recommended", result.getComment());
        assertEquals(4, result.getRating());
        assertEquals(ReviewModerationStatus.ACTIVE, result.getModerationStatus());

        Mockito.verify(profanityFilterService).containsProfanity(inputDto.getComment());
        Mockito.verify(bookRepository).findById(bookId);
        Mockito.verify(userRepository).findById("user-1");
        Mockito.verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("givenProfanity whenAddReviewToBook thenThrowIllegalArgumentException")
    void givenProfanity_whenAddReviewToBook_thenThrowIllegalArgumentException() {
        // Arrange
        Long bookId = 1L;

        ReviewDTO inputDto = new ReviewDTO(
                null,
                "This book is really shit",
                1,
                0,
                null,
                null,
                null,
                null,
                false
        );

        Book book = new Book();
        book.setId(bookId);

        Mockito.when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        Mockito.when(profanityFilterService.containsProfanity(inputDto.getComment())).thenReturn(true);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.addReviewToBook(bookId, inputDto, authentication)
        );

        // Assert
        assertEquals(
                "Je review bevat ongepast taalgebruik en kan niet opgeslagen worden.",
                exception.getMessage()
        );

        Mockito.verify(bookRepository).findById(bookId);
        Mockito.verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("givenInvalidBookId whenAddReviewToBook thenThrowResourceNotFoundException")
    void givenInvalidBookId_whenAddReviewToBook_thenThrowResourceNotFoundException() {
        // Arrange
        Long invalidBookId = 999L;

        ReviewDTO inputDto = new ReviewDTO(
                null,
                "This book does not exist",
                1,
                0,
                null,
                null,
                null,
                null,
                false
        );

        Mockito.when(bookRepository.findById(invalidBookId)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.addReviewToBook(invalidBookId, inputDto, authentication)
        );

        // Assert
        assertEquals("Boek niet gevonden met id: 999", exception.getMessage());
        Mockito.verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("givenValidBookId whenGetAverageRating thenReturnAverageValue")
    void givenValidBookId_whenGetAverageRating_thenReturnAverageValue() {
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
    @DisplayName("givenExistingReviewId whenDeleteReview thenDeleteReview")
    void givenExistingReviewId_whenDeleteReview_thenDeleteReview() {
        // Arrange
        Long reviewId = 1L;
        String userId = "student-1";

        Book book = new Book();
        book.setId(1L);

        User reviewOwner = new User();
        reviewOwner.setId(userId);

        Review review = new Review("Test review", 5, book, reviewOwner);
        review.setId(reviewId);

        Mockito.when(authentication.getName()).thenReturn(userId);
        Mockito.when(authentication.getAuthorities()).thenReturn(List.of());
        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // Act
        reviewService.deleteReview(reviewId, authentication);

        // Assert
        Mockito.verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("givenInvalidReviewId whenDeleteReview thenThrowResourceNotFoundException")
    void givenInvalidReviewId_whenDeleteReview_thenThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 999L;

        Mockito.when(authentication.getName()).thenReturn("user-1");
        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.deleteReview(reviewId, authentication)
        );

        // Assert
        assertEquals("Review niet gevonden met id: 999", exception.getMessage());
        Mockito.verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("givenBookWithoutReviews whenGetReviewsForBook thenReturnEmptyList")
    void givenBookWithoutReviews_whenGetReviewsForBook_thenReturnEmptyList() {
        // Arrange
        Long bookId = 2L;

        Mockito.when(authentication.getName()).thenReturn("user-1");
        Mockito.when(reviewRepository.findByBookId(bookId)).thenReturn(List.of());

        // Act
        List<ReviewDTO> result = reviewService.getReviewsForBook(bookId, authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        Mockito.verify(reviewRepository).findByBookId(bookId);
    }

    @Test
    @DisplayName("givenBookWithoutReviews whenGetAverageRating thenReturnZero")
    void givenBookWithoutReviews_whenGetAverageRating_thenReturnZero() {
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
    @DisplayName("givenExistingReviewAndNewReporter whenReportReview thenCreateReportAndFlagReview")
    void givenExistingReviewAndNewReporter_whenReportReview_thenCreateReportAndFlagReview() {
        // Arrange
        Long reviewId = 1L;
        String userId = "user-1";

        Book book = new Book();
        book.setId(42L);
        book.setTitle("Boektitel");
        book.setAuthor("Auteur");

        Review review = new Review();
        review.setId(reviewId);
        review.setBook(book);
        review.setComment("Good book");
        review.setRating(4);
        review.setReportCount(2);
        review.setModerationStatus(ReviewModerationStatus.ACTIVE);

        User user = new User();
        user.setId(userId);

        Mockito.when(authentication.getName()).thenReturn(userId);
        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(reviewId, userId))
            .thenReturn(false, true);        
        Mockito.when(reviewReportRepository.countByReviewId(reviewId)).thenReturn(3L);
        Mockito.when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewDTO result = reviewService.reportReview(reviewId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(reviewId, result.getId());
        assertEquals("Good book", result.getComment());
        assertEquals(4, result.getRating());
        assertEquals(3, result.getReportCount());
        assertEquals(ReviewModerationStatus.FLAGGED, result.getModerationStatus());
        assertTrue(result.isReportedByCurrentUser());

        Mockito.verify(reviewRepository).findById(reviewId);
        Mockito.verify(userRepository).findById(userId);
        Mockito.verify(reviewReportRepository).save(any(ReviewReport.class));
        Mockito.verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("givenAlreadyReportedReview whenReportReview thenThrowResourceAlreadyExistsException")
    void givenAlreadyReportedReview_whenReportReview_thenThrowResourceAlreadyExistsException() {
        // Arrange
        Long reviewId = 1L;
        String userId = "user-1";

        Book book = new Book();
        book.setId(42L);

        User reviewOwner = new User();
        reviewOwner.setId("student-1");

        Review review = new Review("Good book", 4, book, reviewOwner);
        review.setId(reviewId);

        User reportingUser = new User();
        reportingUser.setId(userId);

        Mockito.when(authentication.getName()).thenReturn(userId);
        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(reportingUser));
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(reviewId, userId)).thenReturn(true);

        // Act
        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> reviewService.reportReview(reviewId, authentication)
        );

        // Assert
        assertEquals("Je hebt deze review al gerapporteerd.", exception.getMessage());
        Mockito.verify(reviewReportRepository, never()).save(any(ReviewReport.class));
        Mockito.verify(reviewRepository, never()).save(any(Review.class));
    }


    @Test
    @DisplayName("whenGetFlaggedReviews thenReturnFlaggedReviewList")
    void whenGetFlaggedReviews_thenReturnFlaggedReviewList() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Titel");
        book.setAuthor("Auteur");

        User reviewOwner = new User();
        reviewOwner.setId("student-1");

        Review flaggedReview = new Review("Ongepaste review", 1, book, reviewOwner);
        flaggedReview.setId(7L);
        flaggedReview.setReportCount(5);
        flaggedReview.setModerationStatus(ReviewModerationStatus.FLAGGED);

        Mockito.when(authentication.getName()).thenReturn("librarian-1");
        Mockito.when(reviewRepository.findByModerationStatus(ReviewModerationStatus.FLAGGED))
                .thenReturn(List.of(flaggedReview));
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(7L, "librarian-1"))
                .thenReturn(false);

        // Act
        List<ReviewDTO> result = reviewService.getFlaggedReviews(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        assertEquals(ReviewModerationStatus.FLAGGED, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("givenFlaggedReview whenKeepReview thenReturnKeptReview")
    void givenFlaggedReview_whenKeepReview_thenReturnKeptReview() {
        // Arrange
        Long reviewId = 5L;
        String userId = "librarian-1";

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Titel");
        book.setAuthor("Auteur");

        User reviewOwner = new User();
        reviewOwner.setId("student-1");

        Review review = new Review("Deze review mag blijven", 4, book, reviewOwner);
        review.setId(reviewId);
        review.setReportCount(3);
        review.setModerationStatus(ReviewModerationStatus.FLAGGED);

        Mockito.when(authentication.getName()).thenReturn(userId);
        Mockito.when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        Mockito.when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(reviewReportRepository.existsByReviewIdAndReportedById(reviewId, userId)).thenReturn(false);

        // Act
        ReviewDTO result = reviewService.keepReview(reviewId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(reviewId, result.getId());
        assertEquals(ReviewModerationStatus.KEPT, result.getModerationStatus());
        Mockito.verify(reviewRepository).save(review);
    }
}