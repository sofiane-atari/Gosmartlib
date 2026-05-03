package edu.ap.bib_backend.service.lessontips;

import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipAttachmentRepository;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipAttachmentMapper;
import edu.ap.bib_backend.service.storage.FileStorageService;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class LessonTipAttachmentService {

    private final LessonTipRepository lessonTipRepository;
    private final LessonTipAttachmentRepository lessonTipAttachmentRepository;
    private final LessonTipAttachmentMapper lessonTipAttachmentMapper;
    private final LessonTipAuthorizationService lessonTipAuthorizationService;
    private final FileStorageService fileStorageService;

    public LessonTipAttachmentService(
            LessonTipRepository lessonTipRepository,
            LessonTipAttachmentRepository lessonTipAttachmentRepository,
            LessonTipAttachmentMapper lessonTipAttachmentMapper,
            LessonTipAuthorizationService lessonTipAuthorizationService,
            FileStorageService fileStorageService
    ) {
        this.lessonTipRepository = lessonTipRepository;
        this.lessonTipAttachmentRepository = lessonTipAttachmentRepository;
        this.lessonTipAttachmentMapper = lessonTipAttachmentMapper;
        this.lessonTipAuthorizationService = lessonTipAuthorizationService;
        this.fileStorageService = fileStorageService;
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
        String storedFileName = fileStorageService.storeFile(file);

        try {
            LessonTipAttachment attachment = buildAttachment(lessonTip, file, storedFileName);
            LessonTipAttachment savedAttachment = lessonTipAttachmentRepository.save(attachment);
            return lessonTipAttachmentMapper.toResponse(savedAttachment);
        } catch (RuntimeException exception) {
            fileStorageService.deleteFile(storedFileName);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public LessonTipAttachmentDownloadDataDto getAttachmentDownloadData(Long lessonTipId, Long attachmentId) {
        LessonTipAttachment attachment = findAttachmentOrThrow(attachmentId, lessonTipId);
        Resource resource = fileStorageService.loadAsResource(attachment.getStoredFileName());

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
        fileStorageService.deleteFile(attachment.getStoredFileName());
    }

    private LessonTip findLessonTipForModification(Long lessonTipId, Authentication authentication) {
        LessonTip lessonTip = findLessonTipOrThrow(lessonTipId);
        lessonTipAuthorizationService.assertCanModify(lessonTip, authentication);
        return lessonTip;
    }

    private LessonTipAttachment buildAttachment(
            LessonTip lessonTip,
            MultipartFile file,
            String storedFileName
    ) {
        LessonTipAttachment attachment = new LessonTipAttachment();
        attachment.setLessonTip(lessonTip);
        attachment.setOriginalFileName(resolveOriginalFileName(file));
        attachment.setStoredFileName(storedFileName);
        attachment.setContentType(resolveContentType(file));
        attachment.setFileSize(file.getSize());
        return attachment;
    }

    private LessonTip findLessonTipOrThrow(Long lessonTipId) {
        return lessonTipRepository.findById(lessonTipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Lestips niet gevonden met id: " + lessonTipId
                ));
    }

    private LessonTipAttachment findAttachmentOrThrow(Long attachmentId, Long lessonTipId) {
        return lessonTipAttachmentRepository.findByIdAndLessonTipId(attachmentId, lessonTipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "attachmentId",
                        "Bijlage niet gevonden met id: " + attachmentId
                ));
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
            return "application/octet-stream";
        }

        return contentType;
    }
}