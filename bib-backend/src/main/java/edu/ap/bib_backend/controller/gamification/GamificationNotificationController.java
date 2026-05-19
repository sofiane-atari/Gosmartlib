package edu.ap.bib_backend.controller.gamification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.service.gamification.GamificationNotificationService;
import edu.ap.bib_backend.service.gamification.sse.GamificationNotificationSseService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/gamification/me/notifications")
public class GamificationNotificationController {

    private final GamificationNotificationService gamificationNotificationService;
    private final GamificationNotificationSseService gamificationNotificationSseService;

    public GamificationNotificationController(
            GamificationNotificationService gamificationNotificationService,
            GamificationNotificationSseService gamificationNotificationSseService
    ) {
        this.gamificationNotificationService = gamificationNotificationService;
        this.gamificationNotificationSseService = gamificationNotificationSseService;
    }

    @GetMapping("/unread")
    public ResponseEntity<List<GamificationNotificationDTO>> getUnreadNotifications(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                gamificationNotificationService.getUnreadNotifications(authentication)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        gamificationNotificationService.markAsRead(authentication, notificationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Opent een SSE-stream voor gamification notificaties van de ingelogde gebruiker.
     * SseEmitter beheert de async context zelf.
     */
    @GetMapping("/stream")
    public SseEmitter streamNotifications(Authentication authentication) {
        String currentUserId = gamificationNotificationService.getCurrentUserId(authentication);
        return gamificationNotificationSseService.subscribe(currentUserId);
    }
}