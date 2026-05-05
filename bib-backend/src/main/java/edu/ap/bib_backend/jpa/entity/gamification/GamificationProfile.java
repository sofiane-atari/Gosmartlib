package edu.ap.bib_backend.jpa.entity.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.entity.User;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gamification_profiles")
@Getter
@Setter
public class GamificationProfile {

    private static final int INITIAL_TOTAL_POINTS = 0;
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_POINTS_UNTIL_NEXT_LEVEL = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Every user has at most one gamification profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int totalPoints = INITIAL_TOTAL_POINTS;

    @Column(nullable = false)
    private int currentLevel = INITIAL_LEVEL;

    @Column(nullable = false)
    private int pointsUntilNextLevel = INITIAL_POINTS_UNTIL_NEXT_LEVEL;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public GamificationProfile() {
    }

    public GamificationProfile(User user) {
        this.user = user;
        this.totalPoints = INITIAL_TOTAL_POINTS;
        this.currentLevel = INITIAL_LEVEL;
        this.pointsUntilNextLevel = INITIAL_POINTS_UNTIL_NEXT_LEVEL;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}