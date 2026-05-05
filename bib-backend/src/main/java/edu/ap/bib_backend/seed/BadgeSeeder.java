package edu.ap.bib_backend.seed;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.ap.bib_backend.jpa.entity.gamification.Badge;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeCategory;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeRarity;
import edu.ap.bib_backend.jpa.enums.gamification.BadgeType;
import edu.ap.bib_backend.jpa.repository.gamification.BadgeRepository;
import edu.ap.bib_backend.service.gamification.util.BadgeCode;

@Configuration
public class BadgeSeeder {

    private static final int FIRST_BOOK_RETURNED_TARGET = 1;
    private static final int FIRST_BOOK_RETURNED_REWARD_POINTS = 10;
    private static final int FIRST_BOOK_RETURNED_DISPLAY_ORDER = 1;

    private static final int BOOKWORM_TARGET = 5;
    private static final int BOOKWORM_REWARD_POINTS = 25;
    private static final int BOOKWORM_DISPLAY_ORDER = 2;

    private static final int FIRST_REVIEW_TARGET = 1;
    private static final int FIRST_REVIEW_REWARD_POINTS = 10;
    private static final int FIRST_REVIEW_DISPLAY_ORDER = 3;

    private static final int ON_TIME_RETURNER_TARGET = 5;
    private static final int ON_TIME_RETURNER_REWARD_POINTS = 25;
    private static final int ON_TIME_RETURNER_DISPLAY_ORDER = 4;

    private static final String FIRST_BOOK_RETURNED_ASSET_KEY = "first-book-returned";
    private static final String BOOKWORM_ASSET_KEY = "bookworm-5";
    private static final String FIRST_REVIEW_ASSET_KEY = "first-review";
    private static final String ON_TIME_RETURNER_ASSET_KEY = "on-time-returner-5";
    private static final String LOCKED_ASSET_SUFFIX = "-locked";

    private static final String COMMON_THEME_COLOR = "#4F8EF7";
    private static final String RARE_THEME_COLOR = "#F28C28";

    @Bean
    CommandLineRunner seedGamificationBadges(BadgeRepository badgeRepository) {
        return args -> {
            List<Badge> badges = List.of(
                    createFirstBookReturnedBadge(),
                    createBookwormBadge(),
                    createFirstReviewBadge(),
                    createOnTimeReturnerBadge()
            );

            badges.stream()
                    .filter(badge -> !badgeRepository.existsByCode(badge.getCode()))
                    .forEach(badgeRepository::save);
        };
    }

    private Badge createFirstBookReturnedBadge() {
        return new Badge(
                BadgeCode.FIRST_BOOK_RETURNED,
                "Eerste boek teruggebracht",
                "Je hebt je eerste boek teruggebracht.",
                BadgeCategory.READING,
                BadgeType.ONE_TIME,
                BadgeRarity.COMMON,
                FIRST_BOOK_RETURNED_TARGET,
                FIRST_BOOK_RETURNED_REWARD_POINTS,
                FIRST_BOOK_RETURNED_ASSET_KEY,
                createLockedAssetKey(FIRST_BOOK_RETURNED_ASSET_KEY),
                COMMON_THEME_COLOR,
                FIRST_BOOK_RETURNED_DISPLAY_ORDER
        );
    }

    private Badge createBookwormBadge() {
        return new Badge(
                BadgeCode.BOOKWORM_5_BOOKS,
                "Boekenwurm",
                "Je hebt vijf boeken teruggebracht.",
                BadgeCategory.READING,
                BadgeType.COUNT_BASED,
                BadgeRarity.RARE,
                BOOKWORM_TARGET,
                BOOKWORM_REWARD_POINTS,
                BOOKWORM_ASSET_KEY,
                createLockedAssetKey(BOOKWORM_ASSET_KEY),
                RARE_THEME_COLOR,
                BOOKWORM_DISPLAY_ORDER
        );
    }

    private Badge createFirstReviewBadge() {
        return new Badge(
                BadgeCode.FIRST_REVIEW,
                "Eerste recensie",
                "Je hebt je eerste recensie geschreven.",
                BadgeCategory.REVIEWING,
                BadgeType.ONE_TIME,
                BadgeRarity.COMMON,
                FIRST_REVIEW_TARGET,
                FIRST_REVIEW_REWARD_POINTS,
                FIRST_REVIEW_ASSET_KEY,
                createLockedAssetKey(FIRST_REVIEW_ASSET_KEY),
                COMMON_THEME_COLOR,
                FIRST_REVIEW_DISPLAY_ORDER
        );
    }

    private Badge createOnTimeReturnerBadge() {
        return new Badge(
                BadgeCode.ON_TIME_RETURNER_5,
                "Stipte lezer",
                "Je hebt vijf boeken op tijd teruggebracht.",
                BadgeCategory.CONSISTENCY,
                BadgeType.COUNT_BASED,
                BadgeRarity.RARE,
                ON_TIME_RETURNER_TARGET,
                ON_TIME_RETURNER_REWARD_POINTS,
                ON_TIME_RETURNER_ASSET_KEY,
                createLockedAssetKey(ON_TIME_RETURNER_ASSET_KEY),
                RARE_THEME_COLOR,
                ON_TIME_RETURNER_DISPLAY_ORDER
        );
    }

    private String createLockedAssetKey(String assetKey) {
        return assetKey + LOCKED_ASSET_SUFFIX;
    }
}