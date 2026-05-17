package edu.ap.bib_backend.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record BookCopyCampusLocationDTO(
        Long campusId,
        String campusName,
        String address,
        Integer postalCode,
        List<BookCopyLocationDTO> copies
) {
}
