package edu.ap.bib_backend.projection.report;

public interface ClassLoanActivityProjection {
    String getClassTitle();

    Long getStudentCount();

    Long getTotalLoans();

    Long getActiveLoans();
}
