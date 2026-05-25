package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoanVolumePeriodDTO(
        @JsonProperty("period") String period,
        @JsonProperty("loanCount") long loanCount) {
}
