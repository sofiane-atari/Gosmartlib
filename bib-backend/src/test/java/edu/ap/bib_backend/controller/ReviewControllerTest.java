package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.service.review.ReviewService;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;

@WebMvcTest(controllers = ReviewController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ReviewService reviewService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private AdminOverrideService adminOverrideService;

        @MockitoBean
        private TokenStorageService tokenStorageService;

        @MockitoBean
        private edu.ap.bib_backend.service.ActiveSchoolContext activeSchoolContext;

        @MockitoBean
        private edu.ap.bib_backend.jpa.repository.SchoolRepository schoolRepository;

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
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.ACTIVE,
                                false,
                                LocalDateTime.now(),
                                "user-1",
                                false,
                                true);

                ReviewDTO review2 = new ReviewDTO(
                                2L,
                                "Beetje saai",
                                3,
                                0,
                                bookId,
                                "Title 1",
                                "Author 1",
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.ACTIVE,
                                false,
                                LocalDateTime.now(),
                                "user-2",
                                false,
                                false);

                Page<ReviewDTO> mockReviews = new PageImpl<>(
                                List.of(review1, review2),
                                PageRequest.of(0, 5),
                                2);

                Mockito.when(reviewService.getReviewsForBook(eq(bookId), eq(0), eq(5), any(Authentication.class)))
                                 .thenReturn(mockReviews);

                // Act
                ResultActions result = mockMvc.perform(
                                get("/reviews/book/{bookId}", bookId)
                                                .principal(new TestingAuthenticationToken("user-1", null))
                                                .contentType(MediaType.APPLICATION_JSON));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[0].id").value(1))
                                .andExpect(jsonPath("$.data[0].comment").value("Geweldig boek!"))
                                .andExpect(jsonPath("$.data[0].rating").value(5))
                                .andExpect(jsonPath("$.data[1].comment").value("Beetje saai"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0))
                                .andExpect(jsonPath("$.meta.pageSize").value(5))
                                .andExpect(jsonPath("$.meta.totalItems").value(2));
        }

        @Test
        @DisplayName("givenValidReviewDTO whenAddReview thenReturn200AndSavedReview")
        void givenValidReviewDTO_whenAddReview_thenReturn200AndSavedReview() throws Exception {
                // Arrange
                Long bookId = 1L;

                ReviewDTO inputReview = new ReviewDTO();
                inputReview.setComment("Echt een aanrader");
                inputReview.setRating(4);

                ReviewDTO savedReview = new ReviewDTO(
                                99L,
                                "Echt een aanrader",
                                4,
                                0,
                                bookId,
                                "Title",
                                "Author",
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.ACTIVE,
                                false,
                                LocalDateTime.now(),
                                "user-1",
                                false,
                                false);

                Mockito.when(reviewService.addReviewToBook(eq(bookId), any(ReviewDTO.class), any(Authentication.class)))
                                .thenReturn(savedReview);

                // Act
                ResultActions result = mockMvc.perform(
                                post("/reviews/book/{bookId}", bookId)
                                                .principal(new TestingAuthenticationToken("user-1", null))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(inputReview)));

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
                                                .contentType(MediaType.APPLICATION_JSON));

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
                                                .principal(new TestingAuthenticationToken("user-1", null)));

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
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.FLAGGED,
                                true,
                                LocalDateTime.now(),
                                "user-1",
                                false,
                                false);

                Mockito.when(reviewService.reportReview(eq(reviewId), any(Authentication.class)))
                                .thenReturn(reviewDto);

                // Act
                ResultActions result = mockMvc.perform(
                                post("/reviews/{reviewId}/report", reviewId)
                                                .principal(new TestingAuthenticationToken("user-1", null)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(reviewId))
                                .andExpect(jsonPath("$.comment").value("Good book"))
                                .andExpect(jsonPath("$.rating").value(4))
                                .andExpect(jsonPath("$.reportCount").value(3));

                Mockito.verify(reviewService).reportReview(eq(reviewId), any(Authentication.class));
        }

        @Test
        @DisplayName("givenValidPaginationParams whenGetFlaggedReviews thenReturn200AndPaginatedResponse")
        void givenValidPaginationParams_whenGetFlaggedReviews_thenReturn200AndPaginatedResponse() throws Exception {
                // Arrange
                ReviewDTO flaggedReview = new ReviewDTO(
                                10L,
                                "Ongepaste review",
                                1,
                                5,
                                2L,
                                "Boektitel",
                                "Auteur",
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.FLAGGED,
                                false,
                                LocalDateTime.now(),
                                "user-1",
                                false, false);

                List<ReviewDTO> reviewList = List.of(flaggedReview);

                Page<ReviewDTO> mockPage = new PageImpl<>(
                                reviewList,
                                PageRequest.of(0, 10),
                                1);

                Mockito.when(reviewService.getFlaggedReviews(any(Authentication.class), eq(0), eq(10)))
                                .thenReturn(mockPage);

                // Act
                ResultActions result = mockMvc.perform(
                                get("/reviews/flagged")
                                                .principal(new TestingAuthenticationToken("librarian-1", null))
                                                .param("page", "0")
                                                .param("size", "10")
                                                .contentType(MediaType.APPLICATION_JSON));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].id").value(10))
                                .andExpect(jsonPath("$.data[0].comment").value("Ongepaste review"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0))
                                .andExpect(jsonPath("$.meta.pageSize").value(10))
                                .andExpect(jsonPath("$.meta.totalItems").value(1))
                                .andExpect(jsonPath("$.meta.totalPages").value(1));
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
                                0,
                                1L,
                                "Titel",
                                "Auteur",
                                "https://example.com/cover.jpg",
                                ReviewModerationStatus.KEPT,
                                false,
                                LocalDateTime.now(),
                                "user-1",
                                false, false);

                Mockito.when(reviewService.keepReview(eq(reviewId), any(Authentication.class)))
                                .thenReturn(keptReview);

                // Act
                ResultActions result = mockMvc.perform(
                                patch("/reviews/{reviewId}/keep", reviewId)
                                                .principal(new TestingAuthenticationToken("librarian-1", null)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(3))
                                .andExpect(jsonPath("$.moderationStatus").value("KEPT"));
        }
}
