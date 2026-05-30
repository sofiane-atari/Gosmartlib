package edu.ap.bib_backend.controller.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.service.gamification.GamificationNotificationService;
import edu.ap.bib_backend.service.gamification.sse.GamificationNotificationSseService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class GamificationNotificationControllerTest {

    private static final Long NOTIFICATION_ID = 1L;

    @Mock
    private GamificationNotificationService gamificationNotificationService;

    @Mock
    private GamificationNotificationSseService gamificationNotificationSseService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GamificationNotificationController gamificationNotificationController;

    @Test
    void givenAuthenticatedUser_whenGetUnreadNotifications_thenReturnsNotifications() {
        // Arrange
        GamificationNotificationDTO notification = new GamificationNotificationDTO(
                NOTIFICATION_ID,
                "BADGE_UNLOCKED",
                "Badge behaald",
                "Je hebt de badge 'Recensie-expert' ontgrendeld.",
                "reviewer-5-reviews",
                "#7C3AED",
                "EPIC",
                10,
                LocalDateTime.now()
        );

        when(gamificationNotificationService.getUnreadNotifications(authentication))
                .thenReturn(List.of(notification));

        // Act
        ResponseEntity<List<GamificationNotificationDTO>> response =
                gamificationNotificationController.getUnreadNotifications(authentication);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(notification);
    }

    @Test
    void givenNotificationId_whenMarkAsRead_thenMarksNotificationAndReturnsNoContent() {
        // Arrange

        // Act
        ResponseEntity<Void> response =
                gamificationNotificationController.markAsRead(authentication, NOTIFICATION_ID);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(gamificationNotificationService).markAsRead(authentication, NOTIFICATION_ID);
    }

    @Test
    void givenAuthenticatedUser_whenStreamNotifications_thenSubscribesCurrentUser() {
        // Arrange
        String userId = "student-user-id";
        SseEmitter emitter = new SseEmitter();

        when(authentication.getName()).thenReturn(userId);
        when(gamificationNotificationSseService.subscribe(userId)).thenReturn(emitter);

        // Act
        SseEmitter result = gamificationNotificationController.streamNotifications(authentication);

        // Assert
        assertThat(result).isSameAs(emitter);
        verify(gamificationNotificationSseService).subscribe(userId);
    }
}
