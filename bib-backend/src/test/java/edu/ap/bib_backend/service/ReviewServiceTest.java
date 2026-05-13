package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.dto.ReviewDashboardStatsDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.entity.review.ReviewReport;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.mapper.ReviewMapper;
import edu.ap.bib_backend.service.review.ReviewService;
import edu.ap.bib_backend.service.review.ReviewValidationService;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private ReviewMapper reviewMapper;

    @Mock
    private ReviewValidationService reviewValidationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReviewService reviewService;

    private static final String CURRENT_USER_ID = "user-1";
    private static final String OTHER_USER_ID = "student-1";

    private Book defaultBook;
    private User currentUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        defaultBook = createBook(1L, "Test Title", "Test Author");
        currentUser = createUser(CURRENT_USER_ID);
        otherUser = createUser(OTHER_USER_ID);

    }

    @Test
    @DisplayName("givenValidBookId whenGetReviewsForBook thenReturnReviewDtoList")
    void givenValidBookId_whenGetReviewsForBook_thenReturnReviewDtoList() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long bookId = 1L;
        List<ReviewModerationStatus> visibleStatuses = List.of(
                ReviewModerationStatus.ACTIVE,
                ReviewModerationStatus.KEPT);

        Review review1 = createReview(10L, "Super boek", 5, defaultBook, currentUser, ReviewModerationStatus.ACTIVE, 0);
        Review review2 = createReview(11L, "Minder goed", 2, defaultBook, currentUser, ReviewModerationStatus.KEPT, 0);

        ReviewDTO reviewDto1 = createReviewDto(10L, "Super boek", 5, 0, defaultBook, ReviewModerationStatus.ACTIVE,
                false);
        ReviewDTO reviewDto2 = createReviewDto(11L, "Minder goed", 2, 0, defaultBook, ReviewModerationStatus.KEPT,
                false);

        when(reviewRepository.findVisibleReviewsByBookId(bookId, visibleStatuses))
                .thenReturn(List.of(review1, review2));
        when(reviewMapper.toDto(review1, CURRENT_USER_ID)).thenReturn(reviewDto1);
        when(reviewMapper.toDto(review2, CURRENT_USER_ID)).thenReturn(reviewDto2);

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

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findVisibleReviewsByBookId(bookId, visibleStatuses);
        verify(reviewMapper).toDto(review1, CURRENT_USER_ID);
        verify(reviewMapper).toDto(review2, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("givenBookWithoutReviews whenGetReviewsForBook thenReturnEmptyList")
    void givenBookWithoutReviews_whenGetReviewsForBook_thenReturnEmptyList() {
        // Arrange
        Long bookId = 2L;
        List<ReviewModerationStatus> visibleStatuses = List.of(
                ReviewModerationStatus.ACTIVE,
                ReviewModerationStatus.KEPT);
        when(reviewRepository.findVisibleReviewsByBookId(bookId, visibleStatuses)).thenReturn(List.of());

        // Act
        List<ReviewDTO> result = reviewService.getReviewsForBook(bookId, authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findVisibleReviewsByBookId(bookId, visibleStatuses);
        verifyNoInteractions(reviewMapper);
    }

    @Test
    @DisplayName("whenGetCurrentUserReviews thenReturnPaginatedReviewDtos")
    void whenGetCurrentUserReviews_thenReturnPaginatedReviewDtos() {
        // Arrange
        when(currentUserService.extractCurrentUserId(authentication))
                .thenReturn(CURRENT_USER_ID);

        Review review = createReview(
                12L,
                "Eigen review",
                4,
                defaultBook,
                currentUser,
                ReviewModerationStatus.ACTIVE,
                0);

        ReviewDTO reviewDto = createReviewDto(
                12L,
                "Eigen review",
                4,
                0,
                defaultBook,
                ReviewModerationStatus.ACTIVE,
                false);

        Page<Review> reviewPage = new PageImpl<>(
                List.of(review),
                PageRequest.of(0, 5),
                1);

        when(reviewRepository.findCurrentUserReviewsWithFlaggedFirst(
                eq(CURRENT_USER_ID),
                any(Pageable.class))).thenReturn(reviewPage);

        when(reviewMapper.toDto(review, CURRENT_USER_ID))
                .thenReturn(reviewDto);

        // Act
        Page<ReviewDTO> result = reviewService.getCurrentUserReviews(authentication, 0, 5);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(12L, result.getContent().get(0).getId());

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findCurrentUserReviewsWithFlaggedFirst(
                eq(CURRENT_USER_ID),
                any(Pageable.class));
        verify(reviewMapper).toDto(review, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("whenGetAllReviews thenReturnReviewDtoList")
    void whenGetAllReviews_thenReturnReviewDtoList() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Review review1 = createReview(10L, "Super boek", 5, defaultBook, otherUser, ReviewModerationStatus.ACTIVE, 0);
        Review review2 = createReview(11L, "Minder goed", 2, defaultBook, otherUser, ReviewModerationStatus.FLAGGED, 3);

        ReviewDTO reviewDto1 = createReviewDto(10L, "Super boek", 5, 0, defaultBook, ReviewModerationStatus.ACTIVE,
                false);
        ReviewDTO reviewDto2 = createReviewDto(11L, "Minder goed", 2, 3, defaultBook, ReviewModerationStatus.FLAGGED,
                true);

        when(reviewRepository.findAll()).thenReturn(List.of(review1, review2));
        when(reviewMapper.toDto(review1, CURRENT_USER_ID)).thenReturn(reviewDto1);
        when(reviewMapper.toDto(review2, CURRENT_USER_ID)).thenReturn(reviewDto2);

        // Act
        List<ReviewDTO> result = reviewService.getAllReviews(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(ReviewModerationStatus.FLAGGED, result.get(1).getModerationStatus());
        assertTrue(result.get(1).isReportedByCurrentUser());

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findAll();
        verify(reviewMapper).toDto(review1, CURRENT_USER_ID);
        verify(reviewMapper).toDto(review2, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("whenGetFlaggedReviewsWithPagination thenReturnPaginatedFlaggedReviews")
    void whenGetFlaggedReviewsWithPagination_thenReturnPaginatedFlaggedReviews() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Review flaggedReview = createReview(7L, "Ongepaste review", 1, defaultBook, otherUser,
                ReviewModerationStatus.FLAGGED, 5);
        ReviewDTO flaggedReviewDto = createReviewDto(7L, "Ongepaste review", 1, 5, defaultBook,
                ReviewModerationStatus.FLAGGED, false);

        Page<Review> reviewPage = new PageImpl<>(
                List.of(flaggedReview),
                PageRequest.of(0, 10),
                1);

        when(reviewRepository.findByModerationStatus(eq(ReviewModerationStatus.FLAGGED), any(Pageable.class)))
                .thenReturn(reviewPage);
        when(reviewMapper.toDto(flaggedReview, CURRENT_USER_ID)).thenReturn(flaggedReviewDto);

        // Act
        Page<ReviewDTO> result = reviewService.getFlaggedReviews(authentication, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(7L, result.getContent().get(0).getId());
        assertEquals(ReviewModerationStatus.FLAGGED, result.getContent().get(0).getModerationStatus());

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findByModerationStatus(eq(ReviewModerationStatus.FLAGGED), any(Pageable.class));
        verify(reviewMapper).toDto(flaggedReview, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("givenValidBookIdAndCleanReviewDto whenAddReviewToBook thenReturnSavedReviewDto")
    void givenValidBookIdAndCleanReviewDto_whenAddReviewToBook_thenReturnSavedReviewDto() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long bookId = 1L;

        ReviewDTO inputDto = new ReviewDTO();
        inputDto.setComment("Highly recommended");
        inputDto.setRating(4);

        Review savedEntity = createReview(99L, "Highly recommended", 4, defaultBook, currentUser,
                ReviewModerationStatus.ACTIVE, 0);
        ReviewDTO savedDto = createReviewDto(99L, "Highly recommended", 4, 0, defaultBook,
                ReviewModerationStatus.ACTIVE, false);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(defaultBook));
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedEntity);
        when(reviewMapper.toDto(savedEntity, CURRENT_USER_ID)).thenReturn(savedDto);

        // Act
        ReviewDTO result = reviewService.addReviewToBook(bookId, inputDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("Highly recommended", result.getComment());
        assertEquals(4, result.getRating());
        assertEquals(ReviewModerationStatus.ACTIVE, result.getModerationStatus());

        verify(reviewValidationService).validateReview(inputDto);
        verify(currentUserService).extractCurrentUserId(authentication);
        verify(bookRepository).findById(bookId);
        verify(userRepository).findById(CURRENT_USER_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewMapper).toDto(savedEntity, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("givenProfanity whenAddReviewToBook thenThrowIllegalArgumentException")
    void givenProfanity_whenAddReviewToBook_thenThrowIllegalArgumentException() {
        // Arrange
        Long bookId = 1L;

        ReviewDTO inputDto = new ReviewDTO();
        inputDto.setComment("This book is really shit");
        inputDto.setRating(1);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(defaultBook));
        doThrow(new IllegalArgumentException(
                "Je review bevat ongepast taalgebruik en kan niet opgeslagen worden.")).when(reviewValidationService)
                .validateReview(inputDto);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.addReviewToBook(bookId, inputDto, authentication));

        // Assert
        assertEquals(
                "Je review bevat ongepast taalgebruik en kan niet opgeslagen worden.",
                exception.getMessage());

        verify(bookRepository).findById(bookId);
        verify(reviewValidationService).validateReview(inputDto);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("givenInvalidBookId whenAddReviewToBook thenThrowResourceNotFoundException")
    void givenInvalidBookId_whenAddReviewToBook_thenThrowResourceNotFoundException() {
        // Arrange
        Long invalidBookId = 999L;

        ReviewDTO inputDto = new ReviewDTO();
        inputDto.setComment("This book does not exist");
        inputDto.setRating(1);

        when(bookRepository.findById(invalidBookId)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.addReviewToBook(invalidBookId, inputDto, authentication));

        // Assert
        assertEquals("Boek niet gevonden met id: 999", exception.getMessage());
        verify(bookRepository).findById(invalidBookId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("givenValidBookId whenGetAverageRating thenReturnAverageValue")
    void givenValidBookId_whenGetAverageRating_thenReturnAverageValue() {
        // Arrange
        Long bookId = 1L;
        Double expectedAverage = 4.5;

        when(reviewRepository.getAverageRatingForBook(bookId)).thenReturn(expectedAverage);

        // Act
        Double result = reviewService.getAverageRating(bookId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedAverage, result);
        verify(reviewRepository).getAverageRatingForBook(bookId);
    }

    @Test
    @DisplayName("givenBookWithoutReviews whenGetAverageRating thenReturnZero")
    void givenBookWithoutReviews_whenGetAverageRating_thenReturnZero() {
        // Arrange
        Long bookId = 3L;
        when(reviewRepository.getAverageRatingForBook(bookId)).thenReturn(0.0);

        // Act
        Double result = reviewService.getAverageRating(bookId);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result);
        verify(reviewRepository).getAverageRatingForBook(bookId);
    }

    @Test
    @DisplayName("givenExistingReviewIdAndOwner whenDeleteReview thenDeleteReview")
    void givenExistingReviewIdAndOwner_whenDeleteReview_thenDeleteReview() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long reviewId = 1L;
        Review review = createReview(reviewId, "Test review", 5, defaultBook, currentUser,
                ReviewModerationStatus.ACTIVE, 0);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(currentUserService.isModerator(authentication)).thenReturn(false);

        // Act
        reviewService.deleteReview(reviewId, authentication);

        // Assert
        verify(currentUserService).extractCurrentUserId(authentication);
        verify(currentUserService).isModerator(authentication);
        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("givenExistingReviewIdAndModerator whenDeleteReview thenDeleteReview")
    void givenExistingReviewIdAndModerator_whenDeleteReview_thenDeleteReview() {
        // Arrange
        Long reviewId = 1L;
        Review review = createReview(reviewId, "Test review", 5, defaultBook, otherUser, ReviewModerationStatus.ACTIVE,
                0);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(currentUserService.isModerator(authentication)).thenReturn(true);

        // Act
        reviewService.deleteReview(reviewId, authentication);

        // Assert
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("givenExistingReviewIdAndUnauthorizedUser whenDeleteReview thenThrowIllegalStateException")
    void givenExistingReviewIdAndUnauthorizedUser_whenDeleteReview_thenThrowIllegalStateException() {
        // Arrange
        Long reviewId = 1L;
        Review review = createReview(reviewId, "Test review", 5, defaultBook, otherUser, ReviewModerationStatus.ACTIVE,
                0);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(currentUserService.isModerator(authentication)).thenReturn(false);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reviewService.deleteReview(reviewId, authentication));

        // Assert
        assertEquals("Je mag alleen je eigen review verwijderen.", exception.getMessage());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("givenInvalidReviewId whenDeleteReview thenThrowResourceNotFoundException")
    void givenInvalidReviewId_whenDeleteReview_thenThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.deleteReview(reviewId, authentication));

        // Assert
        assertEquals("Review niet gevonden met id: 999", exception.getMessage());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("givenExistingReviewAndNewReporter whenReportReview thenCreateReportAndFlagReview")
    void givenExistingReviewAndNewReporter_whenReportReview_thenCreateReportAndFlagReview() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long reviewId = 1L;

        Review review = createReview(reviewId, "Good book", 4, defaultBook, otherUser, ReviewModerationStatus.ACTIVE,
                2);
        Review updatedReview = createReview(reviewId, "Good book", 4, defaultBook, otherUser,
                ReviewModerationStatus.FLAGGED, 3);
        ReviewDTO expectedDto = createReviewDto(reviewId, "Good book", 4, 3, defaultBook,
                ReviewModerationStatus.FLAGGED, true);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reviewReportRepository.existsByReviewIdAndReportedById(reviewId, CURRENT_USER_ID)).thenReturn(false);
        when(reviewReportRepository.countByReviewId(reviewId)).thenReturn(3L);
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);
        when(reviewMapper.toDto(updatedReview, CURRENT_USER_ID)).thenReturn(expectedDto);

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

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findById(reviewId);
        verify(userRepository).findById(CURRENT_USER_ID);
        verify(reviewReportRepository).save(any(ReviewReport.class));
        verify(reviewRepository).save(any(Review.class));
        verify(reviewMapper).toDto(updatedReview, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("givenAlreadyReportedReview whenReportReview thenThrowResourceAlreadyExistsException")
    void givenAlreadyReportedReview_whenReportReview_thenThrowResourceAlreadyExistsException() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long reviewId = 1L;
        Review review = createReview(reviewId, "Good book", 4, defaultBook, otherUser, ReviewModerationStatus.ACTIVE,
                0);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reviewReportRepository.existsByReviewIdAndReportedById(reviewId, CURRENT_USER_ID)).thenReturn(true);

        // Act
        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> reviewService.reportReview(reviewId, authentication));

        // Assert
        assertEquals("Je hebt deze review al gerapporteerd.", exception.getMessage());
        verify(reviewReportRepository, never()).save(any(ReviewReport.class));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("givenFlaggedReview whenKeepReview thenReturnKeptReview")
    void givenFlaggedReview_whenKeepReview_thenReturnKeptReview() {
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(CURRENT_USER_ID);
        // Arrange
        Long reviewId = 5L;

        Review review = createReview(reviewId, "Deze review mag blijven", 4, defaultBook, otherUser,
                ReviewModerationStatus.FLAGGED, 3);
        Review updatedReview = createReview(reviewId, "Deze review mag blijven", 4, defaultBook, otherUser,
                ReviewModerationStatus.KEPT, 3);
        ReviewDTO expectedDto = createReviewDto(reviewId, "Deze review mag blijven", 4, 3, defaultBook,
                ReviewModerationStatus.KEPT, false);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);
        when(reviewMapper.toDto(updatedReview, CURRENT_USER_ID)).thenReturn(expectedDto);

        // Act
        ReviewDTO result = reviewService.keepReview(reviewId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(reviewId, result.getId());
        assertEquals(ReviewModerationStatus.KEPT, result.getModerationStatus());

        verify(currentUserService).extractCurrentUserId(authentication);
        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewMapper).toDto(updatedReview, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("whenGetFlaggedReviewStats thenReturnDashboardStats")
    void whenGetFlaggedReviewStats_thenReturnDashboardStats() {
        // Arrange
        when(reviewRepository.countByModerationStatus(ReviewModerationStatus.FLAGGED)).thenReturn(80L);
        when(reviewRepository.countByModerationStatusAndReportCountGreaterThanEqual(
                ReviewModerationStatus.FLAGGED,
                5)).thenReturn(50L);

        // Act
        ReviewDashboardStatsDTO result = reviewService.getFlaggedReviewStats();

        // Assert
        assertNotNull(result);
        assertEquals(80L, result.totalFlaggedReviews());
        assertEquals(50L, result.criticalFlaggedReviews());

        verify(reviewRepository).countByModerationStatus(ReviewModerationStatus.FLAGGED);
        verify(reviewRepository).countByModerationStatusAndReportCountGreaterThanEqual(
                ReviewModerationStatus.FLAGGED,
                5);
    }

    private Book createBook(Long id, String title, String author) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    private User createUser(String id) {
        User user = new User();
        user.setId(id);
        user.setSchool(createSchool());
        return user;
    }

    private School createSchool() {
        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");
        school.setName("Test School");
        return school;
    }

    private Review createReview(
            Long id,
            String comment,
            int rating,
            Book book,
            User user,
            ReviewModerationStatus moderationStatus,
            int reportCount) {
        Review review = new Review(comment, rating, book, user);
        review.setId(id);
        review.setModerationStatus(moderationStatus);
        review.setReportCount(reportCount);
        return review;
    }

    private ReviewDTO createReviewDto(
            Long id,
            String comment,
            int rating,
            int reportCount,
            Book book,
            ReviewModerationStatus moderationStatus,
            boolean reportedByCurrentUser) {
        return new ReviewDTO(
                id,
                comment,
                rating,
                reportCount,
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                null, // bookImageUrl
                moderationStatus,
                reportedByCurrentUser,
                LocalDateTime.now(),
                null, // reviewerId
                false, // reviewerReviewBlocked
                false);
    }
}
