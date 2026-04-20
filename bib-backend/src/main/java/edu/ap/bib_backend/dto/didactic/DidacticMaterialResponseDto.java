package edu.ap.bib_backend.dto.didactic;

import edu.ap.bib_backend.jpa.enums.DidacticMaterialType;

import java.time.LocalDateTime;
import java.util.List;

public record DidacticMaterialResponseDto(
        Long id,
        String title,
        String description,
        DidacticMaterialType materialType,
        String subject,
        String educationLevel,
        String keywords,
        String createdByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DidacticMaterialBookSummaryResponseDto> linkedBooks
) {
}