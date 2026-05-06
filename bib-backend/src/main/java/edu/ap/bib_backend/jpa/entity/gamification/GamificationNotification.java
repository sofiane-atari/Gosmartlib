package edu.ap.bib_backend.jpa.entity.gamification;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.GamificationNotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "gamification_notifications")
public class GamificationNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private GamificationProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamificationNotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String assetKey;

    @Column(nullable = false)
    private String themeColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeRarity rarity;

    @Column(nullable = false)
    private int rewardPoints;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected GamificationNotification() {
    }

    public GamificationNotification(
            GamificationProfile profile,
            GamificationNotificationType type,
            String title,
            String message,
            String assetKey,
            String themeColor,
            BadgeRarity rarity,
            int rewardPoints
    ) {
        this.profile = profile;
        this.type = type;
        this.title = title;
        this.message = message;
        this.assetKey = assetKey;
        this.themeColor = themeColor;
        this.rarity = rarity;
        this.rewardPoints = rewardPoints;
        this.readAt = null;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }
}