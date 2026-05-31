package edu.ap.bib_backend.controller.lessontips;

import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.service.lessontips.LessonTipAttachmentService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lesson-tips/{lessonTipId}/attachments")
public class LessonTipAttachmentController {

    private final LessonTipAttachmentService lessonTipAttachmentService;

    public LessonTipAttachmentController(
            LessonTipAttachmentService lessonTipAttachmentService
    ) {
        this.lessonTipAttachmentService = lessonTipAttachmentService;
    }

    @GetMapping
    public ResponseEntity<List<LessonTipAttachmentResponseDto>> getAttachments(
            @PathVariable Long lessonTipId
    ) {
        return ResponseEntity.ok(
                lessonTipAttachmentService.getAttachmentsByLessonTipId(lessonTipId)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonTipAttachmentResponseDto> uploadAttachment(
            @PathVariable Long lessonTipId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        LessonTipAttachmentResponseDto uploadedAttachment =
                lessonTipAttachmentService.uploadAttachment(lessonTipId, file, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedAttachment);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(
            @PathVariable Long lessonTipId,
            @PathVariable Long attachmentId
    ) {
        LessonTipAttachmentDownloadDataDto downloadData =
                lessonTipAttachmentService.getAttachmentDownloadData(lessonTipId, attachmentId);

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
            @PathVariable Long lessonTipId,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        lessonTipAttachmentService.deleteAttachment(lessonTipId, attachmentId, authentication);
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
