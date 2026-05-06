package edu.ap.bib_backend.controller.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.gamification.GamificationProfileDTO;
import edu.ap.bib_backend.dto.gamification.GamificationProgressDTO;
import edu.ap.bib_backend.dto.gamification.GamificationRecentEventDTO;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.service.gamification.GamificationQueryService;

@ExtendWith(MockitoExtension.class)
class GamificationControllerTest {

    @Mock
    private GamificationQueryService gamificationQueryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GamificationController gamificationController;

    @Test
    void givenAuthenticatedUser_whenGetMyProfile_thenReturnsProfile() {
        // Arrange
        GamificationProfileDTO profile = new GamificationProfileDTO(
                10,
                1,
                90,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        when(gamificationQueryService.getCurrentUserProfile(authentication))
                .thenReturn(profile);

        // Act
        ResponseEntity<GamificationProfileDTO> response =
                gamificationController.getMyProfile(authentication);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(profile);
    }

    @Test
    void givenBadgeFilters_whenGetMyBadges_thenDelegatesToQueryService() {
        // Arrange
        List<GamificationProgressDTO> badges = List.of();

        when(gamificationQueryService.getCurrentUserBadges(
                authentication,
                BadgeCategory.READING,
                BadgeRarity.RARE,
                false
        )).thenReturn(badges);

        // Act
        ResponseEntity<List<GamificationProgressDTO>> response =
                gamificationController.getMyBadges(
                        authentication,
                        BadgeCategory.READING,
                        BadgeRarity.RARE,
                        false
                );

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(badges);

        verify(gamificationQueryService).getCurrentUserBadges(
                authentication,
                BadgeCategory.READING,
                BadgeRarity.RARE,
                false
        );
    }

    @Test
    void givenAuthenticatedUser_whenGetMyAlmostEarnedBadges_thenReturnsBadges() {
        // Arrange
        List<GamificationProgressDTO> badges = List.of();

        when(gamificationQueryService.getCurrentUserAlmostEarnedBadges(authentication))
                .thenReturn(badges);

        // Act
        ResponseEntity<List<GamificationProgressDTO>> response =
                gamificationController.getMyAlmostEarnedBadges(authentication);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(badges);
    }

    @Test
    void givenPageRequest_whenGetMyRecentEvents_thenReturnsPagedEvents() {
        // Arrange
        int page = 0;
        int size = 5;
        PageDTO<GamificationRecentEventDTO> events = new PageDTO<>(
                List.of(),
                new PaginatedMeta(0, 5, 0, 0)
        );

        when(gamificationQueryService.getCurrentUserRecentEvents(authentication, page, size))
                .thenReturn(events);

        // Act
        ResponseEntity<PageDTO<GamificationRecentEventDTO>> response =
                gamificationController.getMyRecentEvents(authentication, page, size);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(events);
    }
}