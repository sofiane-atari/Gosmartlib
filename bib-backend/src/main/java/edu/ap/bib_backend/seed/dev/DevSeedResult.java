package edu.ap.bib_backend.seed.dev;

public record DevSeedResult(
        int tagsCreated,
        int booksCreated,
        int copiesCreated,
        int loansCreated,
        int loansReturned,
        int reviewsCreated,
        int reviewsReported,
        int readingListsCreated,
        int favoritesAdded,
        int lessonTipsCreated,
        int mailSubscriptionsCreated
) {
    public DevSeedResult add(DevSeedResult other) {
        return new DevSeedResult(
                this.tagsCreated + other.tagsCreated,
                this.booksCreated + other.booksCreated,
                this.copiesCreated + other.copiesCreated,
                this.loansCreated + other.loansCreated,
                this.loansReturned + other.loansReturned,
                this.reviewsCreated + other.reviewsCreated,
                this.reviewsReported + other.reviewsReported,
                this.readingListsCreated + other.readingListsCreated,
                this.favoritesAdded + other.favoritesAdded,
                this.lessonTipsCreated + other.lessonTipsCreated,
                this.mailSubscriptionsCreated + other.mailSubscriptionsCreated
        );
    }
}