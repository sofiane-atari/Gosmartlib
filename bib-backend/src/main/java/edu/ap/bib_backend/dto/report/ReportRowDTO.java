package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportRowDTO(
                @JsonProperty("studentSourcedId") String studentSourcedId,
                @JsonProperty("displayName") String displayName,

                @JsonProperty("totalItems") int totalItems,
                @JsonProperty("completedItems") int completedItems,
                @JsonProperty("progressPercentage") double progressPercentage,
                @JsonProperty("averageScore") double averageScore,
                @JsonProperty("totalTimeMinutes") int totalTimeMinutes,

                @JsonProperty("linkedUserId") String linkedUserId,
                @JsonProperty("totalLoans") long totalLoans,
                @JsonProperty("activeLoans") long activeLoans,
                @JsonProperty("returnedLoans") long returnedLoans,
                @JsonProperty("reviewCount") long reviewCount) {
}