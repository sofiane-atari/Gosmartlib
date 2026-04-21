package edu.ap.bib_backend.service.didactic;

import edu.ap.bib_backend.dto.didactic.DidacticMaterialAttachmentResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialAttachment;
import edu.ap.bib_backend.jpa.repository.didactic.DidacticMaterialAttachmentRepository;
import edu.ap.bib_backend.jpa.repository.didactic.DidacticMaterialRepository;
import edu.ap.bib_backend.mapper.didactic.DidacticMaterialAttachmentMapper;
import edu.ap.bib_backend.service.storage.FileStorageService;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class DidacticMaterialAttachmentService {

    private final DidacticMaterialRepository didacticMaterialRepository;
    private final DidacticMaterialAttachmentRepository didacticMaterialAttachmentRepository;
    private final DidacticMaterialAttachmentMapper didacticMaterialAttachmentMapper;
    private final DidacticMaterialAuthorizationService didacticMaterialAuthorizationService;
    private final FileStorageService fileStorageService;

    public DidacticMaterialAttachmentService(
            DidacticMaterialRepository didacticMaterialRepository,
            DidacticMaterialAttachmentRepository didacticMaterialAttachmentRepository,
            DidacticMaterialAttachmentMapper didacticMaterialAttachmentMapper,
            DidacticMaterialAuthorizationService didacticMaterialAuthorizationService,
            FileStorageService fileStorageService
    ) {
        this.didacticMaterialRepository = didacticMaterialRepository;
        this.didacticMaterialAttachmentRepository = didacticMaterialAttachmentRepository;
        this.didacticMaterialAttachmentMapper = didacticMaterialAttachmentMapper;
        this.didacticMaterialAuthorizationService = didacticMaterialAuthorizationService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<DidacticMaterialAttachmentResponseDto> getAttachmentsByMaterialId(Long didacticMaterialId) {
        findMaterialOrThrow(didacticMaterialId);

        return didacticMaterialAttachmentMapper.toResponseList(
                didacticMaterialAttachmentRepository.findAllByDidacticMaterialIdOrderByUploadedAtDesc(didacticMaterialId)
        );
    }

    public DidacticMaterialAttachmentResponseDto uploadAttachment(
            Long didacticMaterialId,
            MultipartFile file,
            Authentication authentication
    ) {
        DidacticMaterial didacticMaterial = findMaterialOrThrow(didacticMaterialId);
        didacticMaterialAuthorizationService.assertCanModify(didacticMaterial, authentication);

        String storedFileName = fileStorageService.storeFile(file);

        try {
            DidacticMaterialAttachment attachment = new DidacticMaterialAttachment();
            attachment.setDidacticMaterial(didacticMaterial);
            attachment.setOriginalFileName(resolveOriginalFileName(file));
            attachment.setStoredFileName(storedFileName);
            attachment.setContentType(resolveContentType(file));
            attachment.setFileSize(file.getSize());

            DidacticMaterialAttachment savedAttachment =
                    didacticMaterialAttachmentRepository.save(attachment);

            return didacticMaterialAttachmentMapper.toResponse(savedAttachment);
        } catch (RuntimeException exception) {
            fileStorageService.deleteFile(storedFileName);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadDataDto getAttachmentDownloadData(Long didacticMaterialId, Long attachmentId) {
        DidacticMaterialAttachment attachment = findAttachmentOrThrow(attachmentId, didacticMaterialId);
        Resource resource = fileStorageService.loadAsResource(attachment.getStoredFileName());

        return new AttachmentDownloadData(
                resource,
                attachment.getOriginalFileName(),
                attachment.getContentType()
        );
    }

    public void deleteAttachment(Long didacticMaterialId, Long attachmentId, Authentication authentication) {
        DidacticMaterialAttachment attachment = findAttachmentOrThrow(attachmentId, didacticMaterialId);
        didacticMaterialAuthorizationService.assertCanModify(attachment.getDidacticMaterial(), authentication);

        didacticMaterialAttachmentRepository.delete(attachment);
        fileStorageService.deleteFile(attachment.getStoredFileName());
    }

    private DidacticMaterial findMaterialOrThrow(Long didacticMaterialId) {
        return didacticMaterialRepository.findById(didacticMaterialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Didactisch materiaal niet gevonden met id: " + didacticMaterialId
                ));
    }

    private DidacticMaterialAttachment findAttachmentOrThrow(Long attachmentId, Long didacticMaterialId) {
        return didacticMaterialAttachmentRepository.findByIdAndDidacticMaterialId(attachmentId, didacticMaterialId)
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