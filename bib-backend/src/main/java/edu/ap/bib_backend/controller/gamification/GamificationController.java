package edu.ap.bib_backend.controller.gamification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.service.gamification.GamificationQueryService;

@RestController
@RequestMapping("/gamification")
public class GamificationController {

    private final GamificationQueryService gamificationQueryService;

    public GamificationController(GamificationQueryService gamificationQueryService) {
        this.gamificationQueryService = gamificationQueryService;
    }

    @GetMapping("/me")
    public ResponseEntity<GamificationProfileDTO> getMyProfile(Authentication authentication) {
        GamificationProfileDTO profile = gamificationQueryService.getCurrentUserProfile(authentication);

        return ResponseEntity.ok(profile);
    }
}