package edu.ap.bib_backend.dto.loan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.ap.bib_backend.dto.PageDTO;
import lombok.Builder;

@Builder
public record LoanHistoryDTO(
        @JsonProperty("activeLoanCount")
        int activeLoanCount,

        @JsonProperty("returnedLoanCount")
        int returnedLoanCount,

        @JsonProperty("totalLoanedBookCount")
        int totalLoanedBookCount,

        @JsonProperty("activeLoans")
        List<LoanDTO> activeLoans,

        @JsonProperty("returnedLoans")
        PageDTO<LoanDTO> returnedLoans
) {}