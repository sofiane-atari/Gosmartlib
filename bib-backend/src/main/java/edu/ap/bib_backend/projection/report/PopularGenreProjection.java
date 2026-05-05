package edu.ap.bib_backend.projection.report;

public interface PopularGenreProjection {
    Long getGenreId();

    String getName();

    String getColor();

    Long getLoanCount();
}