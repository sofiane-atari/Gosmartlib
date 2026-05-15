package edu.ap.bib_backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.jpa.enums.MailType;
import edu.ap.bib_backend.service.MailSubscriptionService;
import edu.ap.bib_backend.service.user.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class MailSubscriptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MailSubscriptionService mailSubscriptionService;

    @Mock
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        MailSubscriptionController controller = new MailSubscriptionController(
                mailSubscriptionService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
                .build();
    }

    @Test
    void GivenAuthenticatedUser_WhenSubscribeToBook_ThenReturnsCreated() throws Exception {
        when(currentUserService.extractCurrentUserId(any())).thenReturn("user1");
        MailSubscriptionDTO dto = MailSubscriptionDTO.builder()
                .id(1L).bookId(1L).bookTitle("Test Book")
                .mailType(MailType.AVAILABLE_BOOK)
                .createdAt(LocalDateTime.of(2026, 5, 15, 12, 0))
                .build();
        when(mailSubscriptionService.subscribe("user1", 1L)).thenReturn(dto);

        mockMvc.perform(post("/subscriptions/books/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookId").value(1));
    }

    @Test
    void GivenAuthenticatedUser_WhenUnsubscribeFromBook_ThenReturnsNoContent() throws Exception {
        when(currentUserService.extractCurrentUserId(any())).thenReturn("user1");
        doNothing().when(mailSubscriptionService).unsubscribe("user1", 1L);

        mockMvc.perform(delete("/subscriptions/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void GivenAuthenticatedUser_WhenGetMySubscriptions_ThenReturnsList() throws Exception {
        when(currentUserService.extractCurrentUserId(any())).thenReturn("user1");
        MailSubscriptionDTO dto1 = MailSubscriptionDTO.builder().id(1L).bookId(1L).bookTitle("Book 1")
                .mailType(MailType.AVAILABLE_BOOK)
                .createdAt(LocalDateTime.of(2026, 5, 15, 12, 0))
                .build();
        MailSubscriptionDTO dto2 = MailSubscriptionDTO.builder().id(2L).bookId(2L).bookTitle("Book 2")
                .mailType(MailType.AVAILABLE_BOOK)
                .createdAt(LocalDateTime.of(2026, 5, 15, 12, 0))
                .build();
        when(mailSubscriptionService.findSubscriptionsByUser("user1")).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].bookTitle").value("Book 1"))
                .andExpect(jsonPath("$[1].bookTitle").value("Book 2"));
    }

    @Test
    void GivenAuthenticatedUser_WhenGetMySubscriptionsEmpty_ThenReturnsEmptyList() throws Exception {
        when(currentUserService.extractCurrentUserId(any())).thenReturn("user1");
        when(mailSubscriptionService.findSubscriptionsByUser("user1")).thenReturn(List.of());

        mockMvc.perform(get("/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
