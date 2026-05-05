package edu.ap.bib_backend.projection.report;

public interface TopRatedBookProjection {
    Long getBookId();

    String getTitle();

    String getAuthor();

    String getImageUrl();

    Double getAverageRating();

    Long getReviewCount();
}