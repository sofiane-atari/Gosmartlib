package edu.ap.bib_backend.projection.report;

public interface LoanVolumePeriodProjection {
    Integer getYear();

    Integer getMonth();

    Long getLoanCount();
}
