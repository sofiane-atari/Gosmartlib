package edu.ap.bib_backend.dto.loan;

import edu.ap.bib_backend.jpa.enums.LoanStatus;

import java.util.List;

public record LoanGroupDTO(
        String groupId,
        String userId,
        String userDisplayName,
        LoanStatus groupStatus,
        int loanCount,
        int bookCount,
        String earliestReturnDate,
        List<LoanDTO> loans) {
}