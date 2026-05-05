package edu.ap.bib_backend.controller.gamification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
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

    @GetMapping("/me/badges")
    public ResponseEntity<List<GamificationProgressDTO>> getMyBadges(
            Authentication authentication,
            @RequestParam(required = false) BadgeCategory category,
            @RequestParam(required = false) BadgeRarity rarity,
            @RequestParam(required = false) Boolean completed
    ) {
        List<GamificationProgressDTO> badges = gamificationQueryService.getCurrentUserBadges(
                authentication,
                category,
                rarity,
                completed
        );

        return ResponseEntity.ok(badges);
    }
}