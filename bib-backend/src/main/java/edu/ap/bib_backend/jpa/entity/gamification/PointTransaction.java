package edu.ap.bib_backend.jpa.entity.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.PointTransactionType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "gamification_point_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gamification_point_transactions_source",
                        columnNames = {"profile_id", "type", "source_type", "source_id"}
                )
        }
)
@Getter
@Setter
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Profile that received the points.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private GamificationProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private PointTransactionType type;

    @Column(nullable = false)
    private int points;

    /**
     * User-facing Dutch reason, for example:
     * "Boek teruggebracht" or "Badge behaald: Boekenwurm".
     */
    @Column(nullable = false, length = 255)
    private String reason;

    /**
     * Technical source type, for example LOAN, REVIEW or BADGE.
     */
    @Column(name = "source_type", nullable = false, length = 60)
    private String sourceType;

    /**
     * ID of the source entity as string.
     */
    @Column(name = "source_id", nullable = false, length = 80)
    private String sourceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PointTransaction() {
    }

    public PointTransaction(
            GamificationProfile profile,
            PointTransactionType type,
            int points,
            String reason,
            String sourceType,
            String sourceId
    ) {
        this.profile = profile;
        this.type = type;
        this.points = points;
        this.reason = reason;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}