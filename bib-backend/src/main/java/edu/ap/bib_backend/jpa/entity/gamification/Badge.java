package edu.ap.bib_backend.jpa.entity.gamification;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "gamification_badges")
public class Badge {

    private static final boolean DEFAULT_ACTIVE_STATUS = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable technical identifier, for example:
     * FIRST_BOOK_RETURNED, BOOKWORM_5_BOOKS, FIRST_REVIEW.
     */
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    /**
     * User-facing Dutch badge name.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * User-facing Dutch description.
     */
    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BadgeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BadgeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BadgeRarity rarity;

    /**
     * Required progress amount, for example 1, 5, 10.
     */
    @Column(nullable = false)
    private int targetValue;

    /**
     * Extra points awarded when this badge is earned.
     */
    @Column(nullable = false)
    private int rewardPoints;

    /**
     * Asset key used by the frontend, for example:
     * bookworm-5.
     *
     * The backend does not store file paths, SVGs, PNGs or WebP files.
     */
    @Column(nullable = false, length = 120)
    private String assetKey;

    /**
     * Asset key for the locked version of the badge.
     */
    @Column(nullable = false, length = 120)
    private String lockedAssetKey;

    /**
     * Theme color used by the frontend for borders, glow, progress rings, etc.
     */
    @Column(nullable = false, length = 20)
    private String themeColor;

    /**
     * Controls the order in which badges are shown in the frontend.
     */
    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = DEFAULT_ACTIVE_STATUS;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Badge() {
    }

    public Badge(
            String code,
            String name,
            String description,
            BadgeCategory category,
            BadgeType type,
            BadgeRarity rarity,
            int targetValue,
            int rewardPoints,
            String assetKey,
            String lockedAssetKey,
            String themeColor,
            int displayOrder
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.type = type;
        this.rarity = rarity;
        this.targetValue = targetValue;
        this.rewardPoints = rewardPoints;
        this.assetKey = assetKey;
        this.lockedAssetKey = lockedAssetKey;
        this.themeColor = themeColor;
        this.displayOrder = displayOrder;
        this.active = DEFAULT_ACTIVE_STATUS;
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