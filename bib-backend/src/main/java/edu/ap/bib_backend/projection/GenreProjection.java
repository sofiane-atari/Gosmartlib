package edu.ap.bib_backend.projection;

public interface GenreProjection {
    String getName();

    String getColor();

    String getDescription();

    // Default method returns null, allowing JsonInclude to exclude it from JSON
    // When the actual implementation is LabelWithCount, this is overridden to
    // return the actual count
    default Integer getBookCount() {
        return null;
    }
}
