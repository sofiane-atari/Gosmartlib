package edu.ap.bib_backend.controller.gamification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.gamification.GamificationNotificationDTO;
import edu.ap.bib_backend.service.gamification.GamificationNotificationService;

@RestController
@RequestMapping("/gamification/me/notifications")
public class GamificationNotificationController {

    private final GamificationNotificationService gamificationNotificationService;

    public GamificationNotificationController(
            GamificationNotificationService gamificationNotificationService
    ) {
        this.gamificationNotificationService = gamificationNotificationService;
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
}