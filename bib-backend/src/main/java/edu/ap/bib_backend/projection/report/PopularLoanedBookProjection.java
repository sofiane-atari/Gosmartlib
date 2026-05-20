package edu.ap.bib_backend.projection.report;

public interface PopularLoanedBookProjection {
    Long getBookId();

    String getTitle();

    String getAuthor();

    String getImageUrl();

    Long getLoanCount();
}
