package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.ReviewDTO;
import edu.ap.bib_backend.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false) // security filters tijdelijk uit voor pure unit tests
class ReviewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ReviewService reviewService;

        @Test
        @DisplayName("Haal reviews op - Succes")
        void whenGetReviewsForBook_givenValidBookId_thenReturn200AndReviewList() throws Exception {
                // Arrange
                Long bookId = 1L;
                ReviewDTO review1 = new ReviewDTO(1L, "Geweldig boek!", 5, 0);
                ReviewDTO review2 = new ReviewDTO(2L, "Beetje saai", 3, 0);
                List<ReviewDTO> mockReviews = Arrays.asList(review1, review2);

                Mockito.when(reviewService.getReviewsForBook(bookId)).thenReturn(mockReviews);

                // Act
                ResultActions result = mockMvc.perform(get("/api/reviews/book/{bookId}", bookId)
                                .contentType(MediaType.APPLICATION_JSON));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.size()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].comment").value("Geweldig boek!"))
                                .andExpect(jsonPath("$[0].rating").value(5))
                                .andExpect(jsonPath("$[1].comment").value("Beetje saai"));
        }

        @Test
        @DisplayName("Voeg review toe - Succes")
        void whenAddReview_givenValidReviewDTO_thenReturn200AndSavedReview() throws Exception {
                // Arrange
                Long bookId = 1L;
                ReviewDTO inputReview = new ReviewDTO(null, "Echt een aanrader", 4, 0);
                ReviewDTO savedReview = new ReviewDTO(99L, "Echt een aanrader", 4, 0);

                Mockito.when(reviewService.addReviewToBook(eq(bookId), any(ReviewDTO.class)))
                                .thenReturn(savedReview);

                // Act
                ResultActions result = mockMvc.perform(post("/api/reviews/book/{bookId}", bookId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(inputReview)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(99))
                                .andExpect(jsonPath("$.comment").value("Echt een aanrader"))
                                .andExpect(jsonPath("$.rating").value(4));
        }

        @Test
        @DisplayName("Haal gemiddelde rating op - Succes")
        void whenGetAverageRating_givenValidBookId_thenReturn200AndAverageValue() throws Exception {
                // Arrange
                Long bookId = 1L;
                Double mockAverage = 4.2;

                Mockito.when(reviewService.getAverageRating(bookId)).thenReturn(mockAverage);

                // Act
                ResultActions result = mockMvc.perform(get("/api/reviews/book/{bookId}/average", bookId)
                                .contentType(MediaType.APPLICATION_JSON));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(content().string("4.2"));
        }

        @Test
        @DisplayName("Verwijder review - Succes")
        void whenDeleteReview_givenValidId_thenReturn204() throws Exception {
                // Arrange
                Long reviewId = 5L;

                Mockito.doNothing().when(reviewService).deleteReview(reviewId);

                // Act
                ResultActions result = mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId));

                // Assert
                result.andExpect(status().isNoContent());

                Mockito.verify(reviewService).deleteReview(reviewId);
        }
}