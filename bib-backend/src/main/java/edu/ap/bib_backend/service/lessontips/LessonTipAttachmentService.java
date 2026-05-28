package edu.ap.bib_backend.service.lessontips;

import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipAttachmentRepository;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipAttachmentMapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class LessonTipAttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "doc",
            "docx",
            "ppt",
            "pptx",
            "xls",
            "xlsx"
    );

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final LessonTipRepository lessonTipRepository;
    private final LessonTipAttachmentRepository lessonTipAttachmentRepository;
    private final LessonTipAttachmentMapper lessonTipAttachmentMapper;
    private final LessonTipAuthorizationService lessonTipAuthorizationService;

    public LessonTipAttachmentService(
            LessonTipRepository lessonTipRepository,
            LessonTipAttachmentRepository lessonTipAttachmentRepository,
            LessonTipAttachmentMapper lessonTipAttachmentMapper,
            LessonTipAuthorizationService lessonTipAuthorizationService
    ) {
        this.lessonTipRepository = lessonTipRepository;
        this.lessonTipAttachmentRepository = lessonTipAttachmentRepository;
        this.lessonTipAttachmentMapper = lessonTipAttachmentMapper;
        this.lessonTipAuthorizationService = lessonTipAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<LessonTipAttachmentResponseDto> getAttachmentsByLessonTipId(Long lessonTipId) {
        findLessonTipOrThrow(lessonTipId);

        return lessonTipAttachmentMapper.toResponseList(
                lessonTipAttachmentRepository.findAllByLessonTipIdOrderByUploadedAtDesc(lessonTipId)
        );
    }

    public LessonTipAttachmentResponseDto uploadAttachment(
            Long lessonTipId,
            MultipartFile file,
            Authentication authentication
    ) {
        LessonTip lessonTip = findLessonTipForModification(lessonTipId, authentication);
        validateFile(file);

        LessonTipAttachment attachment = buildAttachment(lessonTip, file);
        LessonTipAttachment savedAttachment = lessonTipAttachmentRepository.save(attachment);

        return lessonTipAttachmentMapper.toResponse(savedAttachment);
    }

    @Transactional(readOnly = true)
    public LessonTipAttachmentDownloadDataDto getAttachmentDownloadData(Long lessonTipId, Long attachmentId) {
        LessonTipAttachment attachment = findAttachmentOrThrow(attachmentId, lessonTipId);

        if (attachment.getFileData() == null || attachment.getFileData().length == 0) {
            throw new ResourceNotFoundException(
                    "fileData",
                    "Bestandsinhoud niet gevonden voor bijlage met id: " + attachmentId
            );
        }

        Resource resource = new ByteArrayResource(attachment.getFileData());

        return new LessonTipAttachmentDownloadDataDto(
                resource,
                attachment.getOriginalFileName(),
                attachment.getContentType()
        );
    }

    public void deleteAttachment(Long lessonTipId, Long attachmentId, Authentication authentication) {
        LessonTipAttachment attachment = findAttachmentOrThrow(attachmentId, lessonTipId);
        lessonTipAuthorizationService.assertCanModify(attachment.getLessonTip(), authentication);

        lessonTipAttachmentRepository.delete(attachment);
    }

    private LessonTip findLessonTipForModification(Long lessonTipId, Authentication authentication) {
        LessonTip lessonTip = findLessonTipOrThrow(lessonTipId);
        lessonTipAuthorizationService.assertCanModify(lessonTip, authentication);
        return lessonTip;
    }

    private LessonTipAttachment buildAttachment(
            LessonTip lessonTip,
            MultipartFile file
    ) {
        LessonTipAttachment attachment = new LessonTipAttachment();
        attachment.setLessonTip(lessonTip);
        attachment.setOriginalFileName(resolveOriginalFileName(file));
        attachment.setContentType(resolveContentType(file));
        attachment.setFileSize(file.getSize());
        attachment.setFileData(resolveFileData(file));

        return attachment;
    }

    private LessonTip findLessonTipOrThrow(Long lessonTipId) {
        return lessonTipRepository.findById(lessonTipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Lestip niet gevonden met id: " + lessonTipId
                ));
    }

    private LessonTipAttachment findAttachmentOrThrow(Long attachmentId, Long lessonTipId) {
        return lessonTipAttachmentRepository.findByIdAndLessonTipId(attachmentId, lessonTipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "attachmentId",
                        "Bijlage niet gevonden met id: " + attachmentId
                ));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Er werd geen bestand geselecteerd.");
        }

        String originalFileName = resolveOriginalFileName(file);
        String extension = getFileExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Ongeldig bestandstype. Alleen PDF, Word, Excel en PowerPoint bestanden zijn toegelaten."
            );
        }
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Bestandsnaam is ongeldig.");
        }

        return originalFileName.trim();
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }

        return contentType;
    }

    private byte[] resolveFileData(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Bestand kon niet worden gelezen.", exception);
        }
    }

    private String getFileExtension(String fileName) {
        if (!fileName.contains(".")) {
            throw new IllegalArgumentException("Bestandsnaam is ongeldig.");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (extension.isBlank()) {
            throw new IllegalArgumentException("Bestandsnaam is ongeldig.");
        }

        return extension;
    }
}