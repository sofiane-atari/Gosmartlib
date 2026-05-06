package edu.ap.bib_backend.jpa.entity.gamification;


import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.GamificationNotificationType;
import jakarta.persistence.*;

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

    @Column(nullable = false)
    private boolean read;

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
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public GamificationProfile getProfile() {
        return profile;
    }

    public GamificationNotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public BadgeRarity getRarity() {
        return rarity;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        this.read = true;
    }
}