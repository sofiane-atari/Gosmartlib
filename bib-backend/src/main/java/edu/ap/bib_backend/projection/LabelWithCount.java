package edu.ap.bib_backend.projection;

public interface LabelWithCount extends LabelProjection {
    String getName();

    String getColor();

    String getDescription();

    @Override
    Integer getBookCount();
}
