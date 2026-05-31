package edu.ap.bib_backend.dto;

import edu.ap.bib_backend.jpa.enums.BookCondition;
import jakarta.validation.constraints.NotNull;

public record BookCopyConditionUpdateDTO(
        Long bookCopyId,

        String identifier,

        @NotNull(message = "Boekstaat is verplicht")
        BookCondition condition
) {}
