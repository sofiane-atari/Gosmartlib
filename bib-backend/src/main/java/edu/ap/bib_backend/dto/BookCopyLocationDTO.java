package edu.ap.bib_backend.dto;

import java.time.LocalDate;

import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import lombok.Builder;

@Builder
public record BookCopyLocationDTO(
        Long id,
        Availability availability,
        BookCondition condition,
        LocalDate expectedReturnDate
) {
}
