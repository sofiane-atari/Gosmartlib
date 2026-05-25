package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SystemSchoolActivityDTO(
        @JsonProperty("schoolId") Long schoolId,
        @JsonProperty("schoolName") String schoolName,
        @JsonProperty("logoUrl") String logoUrl,
        @JsonProperty("totalStudents") long totalStudents,
        @JsonProperty("linkedStudents") long linkedStudents,
        @JsonProperty("totalLoans") long totalLoans,
        @JsonProperty("activeLoans") long activeLoans,
        @JsonProperty("averageLoansPerStudent") double averageLoansPerStudent,
        @JsonProperty("lastSyncDate") String lastSyncDate,
        @JsonProperty("syncStatus") String syncStatus,
        @JsonProperty("syncErrorMessage") String syncErrorMessage) {
}
