package edu.ap.bib_backend.dto.didactic;

import org.springframework.core.io.Resource;

public record DidacticMaterialAttachmentDownloadDataDto(
        Resource resource,
        String originalFileName,
        String contentType
) {
}