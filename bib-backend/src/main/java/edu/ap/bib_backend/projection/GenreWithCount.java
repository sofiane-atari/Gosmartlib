package edu.ap.bib_backend.projection;

public interface GenreWithCount extends GenreProjection {
    String getName();

    String getColor();

    String getDescription();

    @Override
    Integer getBookCount();
}
