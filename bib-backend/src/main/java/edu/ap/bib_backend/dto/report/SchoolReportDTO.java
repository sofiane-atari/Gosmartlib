package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SchoolReportDTO(
        @JsonProperty("schoolId") Long schoolId,
        @JsonProperty("schoolName") String schoolName,
        @JsonProperty("selectedCampusId") Long selectedCampusId,
        @JsonProperty("selectedCampusName") String selectedCampusName,
        @JsonProperty("campuses") List<SchoolReportCampusOptionDTO> campuses,
        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("classCount") int classCount,
        @JsonProperty("totalStudentCount") long totalStudentCount,
        @JsonProperty("loansPerStudent") double loansPerStudent,
        @JsonProperty("popularGenres") List<PopularGenreDTO> popularGenres,
        @JsonProperty("topRatedBooks") List<TopRatedBookReportDTO> topRatedBooks,
        @JsonProperty("popularBooks") List<PopularLoanedBookDTO> popularBooks,
        @JsonProperty("loanVolume") List<LoanVolumePeriodDTO> loanVolume,
        @JsonProperty("classActivity") List<SchoolReportClassActivityDTO> classActivity,
        @JsonProperty("returnedOnTimeLoans") long returnedOnTimeLoans,
        @JsonProperty("returnedLateLoans") long returnedLateLoans,
        @JsonProperty("onTimePercentage") double onTimePercentage,
        @JsonProperty("latePercentage") double latePercentage) {
}
