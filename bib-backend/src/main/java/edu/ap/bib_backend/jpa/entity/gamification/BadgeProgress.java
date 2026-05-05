package edu.ap.bib_backend.jpa.entity.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "gamification_badge_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gamification_badge_progress_profile_badge",
                        columnNames = {"profile_id", "badge_id"}
                )
        }
)
@Getter
@Setter
public class BadgeProgress {

    private static final int INITIAL_CURRENT_VALUE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The gamification profile whose progress is tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private GamificationProfile profile;

    /**
     * The badge this progress belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(nullable = false)
    private int currentValue = INITIAL_CURRENT_VALUE;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public BadgeProgress() {
    }

    public BadgeProgress(GamificationProfile profile, Badge badge) {
        this.profile = profile;
        this.badge = badge;
        this.currentValue = INITIAL_CURRENT_VALUE;
        this.completed = false;
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}