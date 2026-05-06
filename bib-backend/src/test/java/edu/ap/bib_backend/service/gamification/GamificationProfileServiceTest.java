package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.exception.gamification.GamificationProfileNotFoundException;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.gamification.GamificationProfileRepository;

@ExtendWith(MockitoExtension.class)
class GamificationProfileServiceTest {

    private static final String USER_ID = "student-user-id";

    @Mock
    private GamificationProfileRepository gamificationProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamificationProfileService gamificationProfileService;

    @Test
    void givenExistingProfile_whenGetOrCreateProfile_thenReturnsExistingProfile() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = new GamificationProfile(user);

        when(gamificationProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        // Act
        GamificationProfile result = gamificationProfileService.getOrCreateProfile(user);

        // Assert
        assertThat(result).isSameAs(profile);
    }

    @Test
    void givenNoExistingProfile_whenGetOrCreateProfile_thenCreatesProfile() {
        // Arrange
        User user = createUser();

        when(gamificationProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(gamificationProfileRepository.save(org.mockito.ArgumentMatchers.any(GamificationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GamificationProfile result = gamificationProfileService.getOrCreateProfile(user);

        // Assert
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getTotalPoints()).isZero();
        assertThat(result.getCurrentLevel()).isEqualTo(1);

        verify(gamificationProfileRepository).save(result);
    }

    @Test
    void givenExistingProfile_whenFindByUserId_thenReturnsProfile() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = new GamificationProfile(user);

        when(gamificationProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        // Act
        GamificationProfile result = gamificationProfileService.findByUserId(USER_ID);

        // Assert
        assertThat(result).isSameAs(profile);
    }

    @Test
    void givenMissingProfile_whenFindByUserId_thenThrowsGamificationProfileNotFoundException() {
        // Arrange
        when(gamificationProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());

        // Act
        GamificationProfileNotFoundException exception = assertThrows(
                GamificationProfileNotFoundException.class,
                () -> gamificationProfileService.findByUserId(USER_ID)
        );

        // Assert
        assertThat(exception).isNotNull();
    }

    @Test
    void givenUserIdWithoutExistingProfile_whenGetOrCreateProfileByUserId_thenCreatesProfile() {
        // Arrange
        User user = createUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gamificationProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(gamificationProfileRepository.save(org.mockito.ArgumentMatchers.any(GamificationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GamificationProfile result = gamificationProfileService.getOrCreateProfileByUserId(USER_ID);

        // Assert
        assertThat(result.getUser()).isSameAs(user);
        verify(gamificationProfileRepository).save(result);
    }

    @Test
    void givenUnknownUserId_whenGetOrCreateProfileByUserId_thenThrowsGamificationProfileNotFoundException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act
        GamificationProfileNotFoundException exception = assertThrows(
                GamificationProfileNotFoundException.class,
                () -> gamificationProfileService.getOrCreateProfileByUserId(USER_ID)
        );

        // Assert
        assertThat(exception).isNotNull();
    }

    @Test
    void givenProfileWithNinetyNinePoints_whenAddPoints_thenUpdatesToLevelTwo() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = new GamificationProfile(user);
        profile.setTotalPoints(99);
        profile.setCurrentLevel(1);
        profile.setPointsUntilNextLevel(1);

        // Act
        gamificationProfileService.addPoints(profile, 1);

        // Assert
        assertThat(profile.getTotalPoints()).isEqualTo(100);
        assertThat(profile.getCurrentLevel()).isEqualTo(2);
        assertThat(profile.getPointsUntilNextLevel()).isEqualTo(100);

        verify(gamificationProfileRepository).save(profile);
    }

    @Test
    void givenProfileWithZeroPoints_whenAddPoints_thenCalculatesPointsUntilNextLevel() {
        // Arrange
        User user = createUser();
        GamificationProfile profile = new GamificationProfile(user);

        // Act
        gamificationProfileService.addPoints(profile, 25);

        // Assert
        assertThat(profile.getTotalPoints()).isEqualTo(25);
        assertThat(profile.getCurrentLevel()).isEqualTo(1);
        assertThat(profile.getPointsUntilNextLevel()).isEqualTo(75);

        verify(gamificationProfileRepository).save(profile);
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }
}