package edu.ap.bib_backend.service.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.jpa.entity.gamification.GamificationProfile;
import edu.ap.bib_backend.jpa.entity.gamification.PointTransaction;
import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import edu.ap.bib_backend.jpa.repository.gamification.PointTransactionRepository;
import edu.ap.bib_backend.service.gamification.util.GamificationSourceType;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final PointTransactionType TRANSACTION_TYPE =
            PointTransactionType.REVIEW_CREATED;

    private static final int POINTS = 5;
    private static final String REASON = "Recensie geschreven";
    private static final String SOURCE_TYPE = GamificationSourceType.REVIEW;
    private static final String SOURCE_ID = "42";

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private GamificationProfileService gamificationProfileService;

    @InjectMocks
    private PointService pointService;

    @Test
    void givenNewPointSource_whenAwardPoints_thenCreatesTransactionAndUpdatesProfile() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();

        when(pointTransactionRepository.existsByProfileAndTypeAndSourceTypeAndSourceId(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        )).thenReturn(false);

        ArgumentCaptor<PointTransaction> transactionCaptor =
                ArgumentCaptor.forClass(PointTransaction.class);

        // Act
        boolean awarded = pointService.awardPoints(
                profile,
                TRANSACTION_TYPE,
                POINTS,
                REASON,
                SOURCE_TYPE,
                SOURCE_ID
        );

        // Assert
        assertThat(awarded).isTrue();

        verify(pointTransactionRepository).save(transactionCaptor.capture());
        PointTransaction savedTransaction = transactionCaptor.getValue();

        assertThat(savedTransaction.getProfile()).isSameAs(profile);
        assertThat(savedTransaction.getType()).isEqualTo(TRANSACTION_TYPE);
        assertThat(savedTransaction.getPoints()).isEqualTo(POINTS);
        assertThat(savedTransaction.getReason()).isEqualTo(REASON);
        assertThat(savedTransaction.getSourceType()).isEqualTo(SOURCE_TYPE);
        assertThat(savedTransaction.getSourceId()).isEqualTo(SOURCE_ID);

        verify(gamificationProfileService).addPoints(profile, POINTS);
    }

    @Test
    void givenAlreadyAwardedPointSource_whenAwardPoints_thenDoesNotCreateTransactionOrUpdateProfile() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();

        when(pointTransactionRepository.existsByProfileAndTypeAndSourceTypeAndSourceId(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        )).thenReturn(true);

        // Act
        boolean awarded = pointService.awardPoints(
                profile,
                TRANSACTION_TYPE,
                POINTS,
                REASON,
                SOURCE_TYPE,
                SOURCE_ID
        );

        // Assert
        assertThat(awarded).isFalse();

        verify(pointTransactionRepository, never()).save(
                org.mockito.ArgumentMatchers.any(PointTransaction.class)
        );
        verify(gamificationProfileService, never()).addPoints(profile, POINTS);
    }

    @Test
    void givenExistingPointSource_whenHasAlreadyAwardedPoints_thenReturnsTrue() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();

        when(pointTransactionRepository.existsByProfileAndTypeAndSourceTypeAndSourceId(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        )).thenReturn(true);

        // Act
        boolean result = pointService.hasAlreadyAwardedPoints(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        );

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void givenNewPointSource_whenHasAlreadyAwardedPoints_thenReturnsFalse() {
        // Arrange
        GamificationProfile profile = new GamificationProfile();

        when(pointTransactionRepository.existsByProfileAndTypeAndSourceTypeAndSourceId(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        )).thenReturn(false);

        // Act
        boolean result = pointService.hasAlreadyAwardedPoints(
                profile,
                TRANSACTION_TYPE,
                SOURCE_TYPE,
                SOURCE_ID
        );

        // Assert
        assertThat(result).isFalse();
    }
}