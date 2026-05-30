package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SystemReportDTO(
        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("totalSchools") long totalSchools,
        @JsonProperty("activeUsers") long activeUsers,
        @JsonProperty("totalBooks") long totalBooks,
        @JsonProperty("totalCopies") long totalCopies,
        @JsonProperty("linkedUsersPercentage") double linkedUsersPercentage,
        @JsonProperty("loanVolume") List<LoanVolumePeriodDTO> loanVolume,
        @JsonProperty("popularBooks") List<PopularLoanedBookDTO> popularBooks,
        @JsonProperty("schools") List<SystemSchoolActivityDTO> schools,
        @JsonProperty("returnedOnTimeLoans") long returnedOnTimeLoans,
        @JsonProperty("returnedLateLoans") long returnedLateLoans,
        @JsonProperty("onTimePercentage") double onTimePercentage,
        @JsonProperty("latePercentage") double latePercentage) {
}
