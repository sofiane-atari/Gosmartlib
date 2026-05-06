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
    private static final int FIRST_BOOK_RETURNED_REWARD_POINTS = 2;
    private static final int FIRST_BOOK_RETURNED_DISPLAY_ORDER = 1;

    private static final int BOOKWORM_TARGET = 5;
    private static final int BOOKWORM_REWARD_POINTS = 5;
    private static final int BOOKWORM_DISPLAY_ORDER = 2;

    private static final int FIRST_REVIEW_TARGET = 1;
    private static final int FIRST_REVIEW_REWARD_POINTS = 2;
    private static final int FIRST_REVIEW_DISPLAY_ORDER = 3;

    private static final int ON_TIME_RETURNER_TARGET = 5;
    private static final int ON_TIME_RETURNER_REWARD_POINTS = 5;
    private static final int ON_TIME_RETURNER_DISPLAY_ORDER = 4;

    private static final int REVIEWER_5_REVIEWS_TARGET = 5;
    private static final int REVIEWER_5_REVIEWS_REWARD_POINTS = 10;
    private static final int REVIEWER_5_REVIEWS_DISPLAY_ORDER = 5;

    private static final int BOOK_RETURNER_10_TARGET = 10;
    private static final int BOOK_RETURNER_10_REWARD_POINTS = 10;
    private static final int BOOK_RETURNER_10_DISPLAY_ORDER = 6;

    private static final int ON_TIME_RETURNER_10_TARGET = 10;
    private static final int ON_TIME_RETURNER_10_REWARD_POINTS = 10;
    private static final int ON_TIME_RETURNER_10_DISPLAY_ORDER = 7;

    private static final int LEGENDARY_READER_25_TARGET = 25;
    private static final int LEGENDARY_READER_25_REWARD_POINTS = 20;
    private static final int LEGENDARY_READER_25_DISPLAY_ORDER = 8;

    private static final String FIRST_BOOK_RETURNED_ASSET_KEY = "first-book-returned";
    private static final String BOOKWORM_ASSET_KEY = "bookworm-5";
    private static final String FIRST_REVIEW_ASSET_KEY = "first-review";
    private static final String ON_TIME_RETURNER_ASSET_KEY = "on-time-returner-5";
    private static final String REVIEWER_5_REVIEWS_ASSET_KEY = "reviewer-5-reviews";
    private static final String BOOK_RETURNER_10_ASSET_KEY = "book-returner-10";
    private static final String ON_TIME_RETURNER_10_ASSET_KEY = "on-time-returner-10";
    private static final String LEGENDARY_READER_25_ASSET_KEY = "legendary-reader-25";

    private static final String LOCKED_ASSET_SUFFIX = "-locked";

    private static final String COMMON_THEME_COLOR = "#4F8EF7";
    private static final String RARE_THEME_COLOR = "#F28C28";
    private static final String EPIC_THEME_COLOR = "#7C3AED";
    private static final String LEGENDARY_THEME_COLOR = "#D97706";

    @Bean
    CommandLineRunner seedGamificationBadges(BadgeRepository badgeRepository) {
        return args -> {
            List<Badge> badges = List.of(
                    createFirstBookReturnedBadge(),
                    createBookwormBadge(),
                    createFirstReviewBadge(),
                    createOnTimeReturnerBadge(),
                    createReviewerFiveReviewsBadge(),
                    createBookReturnerTenBadge(),
                    createOnTimeReturnerTenBadge(),
                    createLegendaryReaderBadge()
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

    private Badge createReviewerFiveReviewsBadge() {
        return new Badge(
                BadgeCode.REVIEWER_5_REVIEWS,
                "Recensie-expert",
                "Je hebt vijf recensies geschreven.",
                BadgeCategory.REVIEWING,
                BadgeType.COUNT_BASED,
                BadgeRarity.EPIC,
                REVIEWER_5_REVIEWS_TARGET,
                REVIEWER_5_REVIEWS_REWARD_POINTS,
                REVIEWER_5_REVIEWS_ASSET_KEY,
                createLockedAssetKey(REVIEWER_5_REVIEWS_ASSET_KEY),
                EPIC_THEME_COLOR,
                REVIEWER_5_REVIEWS_DISPLAY_ORDER
        );
    }

    private Badge createBookReturnerTenBadge() {
        return new Badge(
                BadgeCode.BOOK_RETURNER_10,
                "Toegewijde lezer",
                "Je hebt tien boeken teruggebracht.",
                BadgeCategory.READING,
                BadgeType.COUNT_BASED,
                BadgeRarity.EPIC,
                BOOK_RETURNER_10_TARGET,
                BOOK_RETURNER_10_REWARD_POINTS,
                BOOK_RETURNER_10_ASSET_KEY,
                createLockedAssetKey(BOOK_RETURNER_10_ASSET_KEY),
                EPIC_THEME_COLOR,
                BOOK_RETURNER_10_DISPLAY_ORDER
        );
    }

    private Badge createOnTimeReturnerTenBadge() {
        return new Badge(
                BadgeCode.ON_TIME_RETURNER_10,
                "Meester van stiptheid",
                "Je hebt tien boeken op tijd teruggebracht.",
                BadgeCategory.CONSISTENCY,
                BadgeType.COUNT_BASED,
                BadgeRarity.EPIC,
                ON_TIME_RETURNER_10_TARGET,
                ON_TIME_RETURNER_10_REWARD_POINTS,
                ON_TIME_RETURNER_10_ASSET_KEY,
                createLockedAssetKey(ON_TIME_RETURNER_10_ASSET_KEY),
                EPIC_THEME_COLOR,
                ON_TIME_RETURNER_10_DISPLAY_ORDER
        );
    }

    private Badge createLegendaryReaderBadge() {
        return new Badge(
                BadgeCode.LEGENDARY_READER_25,
                "Legendarische lezer",
                "Je hebt vijfentwintig boeken teruggebracht.",
                BadgeCategory.READING,
                BadgeType.COUNT_BASED,
                BadgeRarity.LEGENDARY,
                LEGENDARY_READER_25_TARGET,
                LEGENDARY_READER_25_REWARD_POINTS,
                LEGENDARY_READER_25_ASSET_KEY,
                createLockedAssetKey(LEGENDARY_READER_25_ASSET_KEY),
                LEGENDARY_THEME_COLOR,
                LEGENDARY_READER_25_DISPLAY_ORDER
        );
    }

    private String createLockedAssetKey(String assetKey) {
        return assetKey + LOCKED_ASSET_SUFFIX;
    }
}