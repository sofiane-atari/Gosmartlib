package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.ReviewService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.TokenStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenStorageService tokenStorageService;

    @MockBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    @DisplayName("givenValidBookId whenGetReviewsForBook thenReturn200AndReviewList")
    void givenValidBookId_whenGetReviewsForBook_thenReturn200AndReviewList() throws Exception {
        // Arrange
        Long bookId = 1L;

        ReviewDTO review1 = new ReviewDTO(
                1L,
                "Geweldig boek!",
                5,
                0,
                bookId,
                "Title 1",
                "Author 1",
                ReviewModerationStatus.ACTIVE,
                false
        );

        ReviewDTO review2 = new ReviewDTO(
                2L,
                "Beetje saai",
                3,
                0,
                bookId,
                "Title 1",
                "Author 1",
                ReviewModerationStatus.ACTIVE,
                false
        );

        List<ReviewDTO> mockReviews = Arrays.asList(review1, review2);

        Mockito.when(reviewService.getReviewsForBook(eq(bookId), any(Authentication.class)))
                .thenReturn(mockReviews);

        // Act
        ResultActions result = mockMvc.perform(
                get("/reviews/book/{bookId}", bookId)
                        .principal(new TestingAuthenticationToken("user-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].comment").value("Geweldig boek!"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[1].comment").value("Beetje saai"));
    }

    @Test
    @DisplayName("whenGetAllReviews thenReturn200AndReviewList")
    void whenGetAllReviews_thenReturn200AndReviewList() throws Exception {
        // Arrange
        ReviewDTO review1 = new ReviewDTO(
                1L,
                "Geweldig boek!",
                5,
                0,
                1L,
                "Title 1",
                "Author 1",
                ReviewModerationStatus.ACTIVE,
                false
        );

        ReviewDTO review2 = new ReviewDTO(
                2L,
                "Beetje saai",
                3,
                0,
                1L,
                "Title 1",
                "Author 1",
                ReviewModerationStatus.ACTIVE,
                false
        );

        List<ReviewDTO> mockReviews = Arrays.asList(review1, review2);

        Mockito.when(reviewService.getAllReviews(any(Authentication.class)))
                .thenReturn(mockReviews);

        // Act
        ResultActions result = mockMvc.perform(
                get("/reviews")
                        .principal(new TestingAuthenticationToken("user-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].comment").value("Geweldig boek!"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[1].comment").value("Beetje saai"));
    }

    @Test
    @DisplayName("givenValidReviewDTO whenAddReview thenReturn200AndSavedReview")
    void givenValidReviewDTO_whenAddReview_thenReturn200AndSavedReview() throws Exception {
        // Arrange
        Long bookId = 1L;

        ReviewDTO inputReview = new ReviewDTO(
                null,
                "Echt een aanrader",
                4,
                0,
                null,
                null,
                null,
                null,
                false
        );

        ReviewDTO savedReview = new ReviewDTO(
                99L,
                "Echt een aanrader",
                4,
                0,
                bookId,
                "Title",
                "Author",
                ReviewModerationStatus.ACTIVE,
                false
        );

        Mockito.when(reviewService.addReviewToBook(eq(bookId), any(ReviewDTO.class), any(Authentication.class)))
                .thenReturn(savedReview);

        // Act
        ResultActions result = mockMvc.perform(
                post("/reviews/book/{bookId}", bookId)
                        .principal(new TestingAuthenticationToken("user-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputReview))
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.comment").value("Echt een aanrader"))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @DisplayName("givenValidBookId whenGetAverageRating thenReturn200AndAverageValue")
    void givenValidBookId_whenGetAverageRating_thenReturn200AndAverageValue() throws Exception {
        // Arrange
        Long bookId = 1L;
        Double mockAverage = 4.2;

        Mockito.when(reviewService.getAverageRating(bookId)).thenReturn(mockAverage);

        // Act
        ResultActions result = mockMvc.perform(
                get("/reviews/book/{bookId}/average", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().string("4.2"));
    }

    @Test
        @DisplayName("givenValidId whenDeleteReview thenReturn204")
        void givenValidId_whenDeleteReview_thenReturn204() throws Exception {
        // Arrange
        Long reviewId = 5L;

        Mockito.doNothing().when(reviewService).deleteReview(eq(reviewId), any(Authentication.class));

        // Act
        ResultActions result = mockMvc.perform(
                delete("/reviews/{reviewId}", reviewId)
                        .principal(new TestingAuthenticationToken("user-1", null))
        );

        // Assert
        result.andExpect(status().isNoContent());

        Mockito.verify(reviewService).deleteReview(eq(reviewId), any(Authentication.class));
        }

    @Test
    @DisplayName("givenValidId whenReportReview thenReturnReportedReview")
    void givenValidId_whenReportReview_thenReturnReportedReview() throws Exception {
        // Arrange
        Long reviewId = 1L;

        ReviewDTO reviewDto = new ReviewDTO(
                reviewId,
                "Good book",
                4,
                3,
                1L,
                "Title",
                "Author",
                ReviewModerationStatus.FLAGGED,
                true
        );

        Mockito.when(reviewService.reportReview(eq(reviewId), any(Authentication.class)))
                .thenReturn(reviewDto);

        // Act
        ResultActions result = mockMvc.perform(
                post("/reviews/{reviewId}/report", reviewId)
                        .principal(new TestingAuthenticationToken("user-1", null))
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.comment").value("Good book"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reportCount").value(3));

        Mockito.verify(reviewService).reportReview(eq(reviewId), any(Authentication.class));
    }

    @Test
    @DisplayName("whenGetFlaggedReviews thenReturn200AndFlaggedReviewList")
    void whenGetFlaggedReviews_thenReturn200AndFlaggedReviewList() throws Exception {
        // Arrange
        ReviewDTO flaggedReview = new ReviewDTO(
                10L,
                "Ongepaste review",
                1,
                5,
                2L,
                "Boektitel",
                "Auteur",
                ReviewModerationStatus.FLAGGED,
                false
        );

        Mockito.when(reviewService.getFlaggedReviews(any(Authentication.class)))
                .thenReturn(List.of(flaggedReview));

        // Act
        ResultActions result = mockMvc.perform(
                get("/reviews/flagged")
                        .principal(new TestingAuthenticationToken("librarian-1", null))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].moderationStatus").value("FLAGGED"));
    }

    @Test
    @DisplayName("givenValidReviewId whenKeepReview thenReturn200AndKeptReview")
    void givenValidReviewId_whenKeepReview_thenReturn200AndKeptReview() throws Exception {
        // Arrange
        Long reviewId = 3L;

        ReviewDTO keptReview = new ReviewDTO(
                reviewId,
                "Deze review mag blijven",
                4,
                3,
                1L,
                "Titel",
                "Auteur",
                ReviewModerationStatus.KEPT,
                false
        );

        Mockito.when(reviewService.keepReview(eq(reviewId), any(Authentication.class)))
                .thenReturn(keptReview);

        // Act
        ResultActions result = mockMvc.perform(
                patch("/reviews/{reviewId}/keep", reviewId)
                        .principal(new TestingAuthenticationToken("librarian-1", null))
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.moderationStatus").value("KEPT"));
    }
}