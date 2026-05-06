package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CampusReportDTO(
        @JsonProperty("campusId") Long campusId,
        @JsonProperty("campusSourcedId") String campusSourcedId,
        @JsonProperty("campusName") String campusName,

        @JsonProperty("classCount") int classCount,
        @JsonProperty("totalStudentCount") int totalStudentCount,
        @JsonProperty("linkedStudentCount") int linkedStudentCount,

        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("averageLoansPerLinkedStudent") double averageLoansPerLinkedStudent,

        @JsonProperty("campusAverageScore") double campusAverageScore,
        @JsonProperty("campusAverageProgress") double campusAverageProgress,

        @JsonProperty("popularGenres") List<PopularGenreDTO> popularGenres,
        @JsonProperty("topRatedBooks") List<TopRatedBookReportDTO> topRatedBooks,
        @JsonProperty("classes") List<CampusReportRowDTO> classes) {
}


