package edu.ap.bib_backend.dto.loan;

import java.util.List;

import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;

public record LoanReturnDTO(
        List<BookCopyConditionUpdateDTO> copyConditions
) {}
