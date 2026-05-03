package edu.ap.bib_backend.dto.lessontips;

import edu.ap.bib_backend.jpa.enums.LessonTipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateLessonTipRequestDto(
        @NotBlank(message = "Titel is verplicht")
        @Size(max = 150, message = "Titel mag maximaal 150 tekens bevatten")
        String title,

        @NotBlank(message = "Beschrijving is verplicht")
        String description,

        @NotNull(message = "Type is verplicht")
        LessonTipType lessonTipType,

        @Size(max = 100, message = "Vak mag maximaal 100 tekens bevatten")
        String subject,

        @Size(max = 100, message = "Onderwijsniveau mag maximaal 100 tekens bevatten")
        String educationLevel,

        @Size(max = 255, message = "Trefwoorden mogen maximaal 255 tekens bevatten")
        String keywords,

        List<Long> linkedBookIds
) {
}