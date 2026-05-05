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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "gamification_user_badges",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gamification_user_badges_profile_badge",
                        columnNames = {"profile_id", "badge_id"}
                )
        }
)
@Getter
@Setter
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The gamification profile that earned this badge.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private GamificationProfile profile;

    /**
     * The badge that was earned.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    public UserBadge() {
    }

    public UserBadge(GamificationProfile profile, Badge badge) {
        this.profile = profile;
        this.badge = badge;
    }

    @PrePersist
    protected void onCreate() {
        this.earnedAt = LocalDateTime.now();
    }
}