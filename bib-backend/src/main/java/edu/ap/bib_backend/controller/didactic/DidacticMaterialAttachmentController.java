package edu.ap.bib_backend.controller.didactic;

import edu.ap.bib_backend.dto.didactic.DidacticMaterialAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialAttachmentResponseDto;
import edu.ap.bib_backend.service.didactic.DidacticMaterialAttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/didactic-materials/{didacticMaterialId}/attachments")
@PreAuthorize("hasAnyRole('TEACHER','LIBRARIAN','ADMIN')")
public class DidacticMaterialAttachmentController {

    private final DidacticMaterialAttachmentService didacticMaterialAttachmentService;

    public DidacticMaterialAttachmentController(
            DidacticMaterialAttachmentService didacticMaterialAttachmentService
    ) {
        this.didacticMaterialAttachmentService = didacticMaterialAttachmentService;
    }

    @GetMapping
    public ResponseEntity<List<DidacticMaterialAttachmentResponseDto>> getAttachments(
            @PathVariable Long didacticMaterialId
    ) {
        return ResponseEntity.ok(
                didacticMaterialAttachmentService.getAttachmentsByMaterialId(didacticMaterialId)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DidacticMaterialAttachmentResponseDto> uploadAttachment(
            @PathVariable Long didacticMaterialId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        DidacticMaterialAttachmentResponseDto uploadedAttachment =
                didacticMaterialAttachmentService.uploadAttachment(didacticMaterialId, file, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedAttachment);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(
            @PathVariable Long didacticMaterialId,
            @PathVariable Long attachmentId
    ) {
        DidacticMaterialAttachmentDownloadDataDto downloadData =
                didacticMaterialAttachmentService.getAttachmentDownloadData(didacticMaterialId, attachmentId);

        MediaType mediaType = resolveMediaType(downloadData.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(downloadData.originalFileName())
                                .build()
                                .toString()
                )
                .body(downloadData.resource());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long didacticMaterialId,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        didacticMaterialAttachmentService.deleteAttachment(didacticMaterialId, attachmentId, authentication);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}