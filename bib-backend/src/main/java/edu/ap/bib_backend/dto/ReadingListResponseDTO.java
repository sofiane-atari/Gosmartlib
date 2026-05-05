package edu.ap.bib_backend.dto;

import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;

import java.util.List;

public record ReadingListResponseDTO(
        Long id,
        String name,
        String userId,
        Long campusId,
        String campusName,
        ReadingListTargetType targetType,
        String targetValue,
        String publicShareId,
        boolean shareEnabled,
        List<ReadingListBookResponseDTO> books
) {}