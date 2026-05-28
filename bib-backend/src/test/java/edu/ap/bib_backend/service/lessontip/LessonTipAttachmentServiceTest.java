package edu.ap.bib_backend.service.lessontip;

import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipAttachmentRepository;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipAttachmentMapper;
import edu.ap.bib_backend.service.lessontips.LessonTipAttachmentService;
import edu.ap.bib_backend.service.lessontips.LessonTipAuthorizationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LessonTipAttachmentServiceTest {

    private static final Long LESSON_TIP_ID = 7L;
    private static final Long ATTACHMENT_ID = 9L;
    private static final byte[] FILE_DATA = "pdf-content".getBytes(StandardCharsets.UTF_8);

    @Mock
    private LessonTipRepository lessonTipRepository;

    @Mock
    private LessonTipAttachmentRepository lessonTipAttachmentRepository;

    @Mock
    private LessonTipAttachmentMapper lessonTipAttachmentMapper;

    @Mock
    private LessonTipAuthorizationService lessonTipAuthorizationService;

    @Mock
    private MultipartFile file;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LessonTipAttachmentService lessonTipAttachmentService;

    @Test
    void givenExistingLessonTipId_whenGetAttachmentsByLessonTipId_thenReturnMappedAttachments() {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment firstAttachment = createAttachment(1L, lessonTip, "first.pdf");
        LessonTipAttachment secondAttachment = createAttachment(2L, lessonTip, "second.pdf");

        LessonTipAttachmentResponseDto firstResponse = createAttachmentResponseDto(1L, "first.pdf");
        LessonTipAttachmentResponseDto secondResponse = createAttachmentResponseDto(2L, "second.pdf");

        List<LessonTipAttachment> attachments = List.of(firstAttachment, secondAttachment);
        List<LessonTipAttachmentResponseDto> expectedResponses = List.of(firstResponse, secondResponse);

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(lessonTipAttachmentRepository.findAllByLessonTipIdOrderByUploadedAtDesc(LESSON_TIP_ID))
                .willReturn(attachments);
        given(lessonTipAttachmentMapper.toResponseList(attachments))
                .willReturn(expectedResponses);

        // Act
        List<LessonTipAttachmentResponseDto> result =
                lessonTipAttachmentService.getAttachmentsByLessonTipId(LESSON_TIP_ID);

        // Assert
        assertThat(result).containsExactly(firstResponse, secondResponse);

        then(lessonTipRepository).should().findById(LESSON_TIP_ID);
        then(lessonTipAttachmentRepository).should()
                .findAllByLessonTipIdOrderByUploadedAtDesc(LESSON_TIP_ID);
        then(lessonTipAttachmentMapper).should().toResponseList(attachments);
    }

    @Test
    void givenUnknownLessonTipId_whenGetAttachmentsByLessonTipId_thenThrowResourceNotFoundException() {
        // Arrange
        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.empty());

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> lessonTipAttachmentService.getAttachmentsByLessonTipId(LESSON_TIP_ID)
        );

        // Assert
        then(lessonTipRepository).should().findById(LESSON_TIP_ID);
        then(lessonTipAttachmentRepository).should(never())
                .findAllByLessonTipIdOrderByUploadedAtDesc(any());
    }

    @Test
    void givenValidFileAndAuthorizedUser_whenUploadAttachment_thenSaveAttachmentAndReturnMappedResponse()
            throws IOException {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment savedAttachment = createAttachment(
                ATTACHMENT_ID,
                lessonTip,
                "lesfiche.pdf"
        );
        LessonTipAttachmentResponseDto responseDto = createAttachmentResponseDto(ATTACHMENT_ID, "lesfiche.pdf");

        ArgumentCaptor<LessonTipAttachment> attachmentCaptor =
                ArgumentCaptor.forClass(LessonTipAttachment.class);

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(file.getOriginalFilename())
                .willReturn("  lesfiche.pdf  ");
        given(file.getContentType())
                .willReturn("application/pdf");
        given(file.getSize())
                .willReturn(2048L);
        given(file.getBytes())
                .willReturn(FILE_DATA);
        given(lessonTipAttachmentRepository.existsByLessonTipIdAndOriginalFileNameIgnoreCaseAndFileSize(
                LESSON_TIP_ID,
                "lesfiche.pdf",
                2048L
        )).willReturn(false);
        given(lessonTipAttachmentRepository.save(any(LessonTipAttachment.class)))
                .willReturn(savedAttachment);
        given(lessonTipAttachmentMapper.toResponse(savedAttachment))
                .willReturn(responseDto);

        // Act
        LessonTipAttachmentResponseDto result =
                lessonTipAttachmentService.uploadAttachment(LESSON_TIP_ID, file, authentication);

        // Assert
        assertThat(result).isEqualTo(responseDto);

        then(lessonTipAuthorizationService).should().assertCanModify(lessonTip, authentication);
        then(lessonTipAttachmentRepository).should().save(attachmentCaptor.capture());

        LessonTipAttachment capturedAttachment = attachmentCaptor.getValue();
        assertThat(capturedAttachment.getLessonTip()).isEqualTo(lessonTip);
        assertThat(capturedAttachment.getOriginalFileName()).isEqualTo("lesfiche.pdf");
        assertThat(capturedAttachment.getContentType()).isEqualTo("application/pdf");
        assertThat(capturedAttachment.getFileSize()).isEqualTo(2048L);
        assertThat(capturedAttachment.getFileData()).isEqualTo(FILE_DATA);
    }

    @Test
    void givenBlankContentType_whenUploadAttachment_thenFallbackToApplicationOctetStream()
            throws IOException {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment savedAttachment = createAttachment(
                ATTACHMENT_ID,
                lessonTip,
                "bestand.pdf"
        );

        ArgumentCaptor<LessonTipAttachment> attachmentCaptor =
                ArgumentCaptor.forClass(LessonTipAttachment.class);

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(file.getOriginalFilename())
                .willReturn("bestand.pdf");
        given(file.getContentType())
                .willReturn(" ");
        given(file.getSize())
                .willReturn(50L);
        given(file.getBytes())
                .willReturn(FILE_DATA);
        given(lessonTipAttachmentRepository.existsByLessonTipIdAndOriginalFileNameIgnoreCaseAndFileSize(
                LESSON_TIP_ID,
                "bestand.pdf",
                50L
        )).willReturn(false);
        given(lessonTipAttachmentRepository.save(any(LessonTipAttachment.class)))
                .willReturn(savedAttachment);
        given(lessonTipAttachmentMapper.toResponse(savedAttachment))
                .willReturn(createAttachmentResponseDto(ATTACHMENT_ID, "bestand.pdf"));

        // Act
        lessonTipAttachmentService.uploadAttachment(LESSON_TIP_ID, file, authentication);

        // Assert
        then(lessonTipAttachmentRepository).should().save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void givenMissingOriginalFileName_whenUploadAttachment_thenThrowIllegalArgumentException() {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(file.getOriginalFilename())
                .willReturn(" ");

        // Act
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonTipAttachmentService.uploadAttachment(LESSON_TIP_ID, file, authentication)
        );

        // Assert
        then(lessonTipAuthorizationService).should().assertCanModify(lessonTip, authentication);
        then(lessonTipAttachmentRepository).should(never()).save(any());
    }

    @Test
    void givenRepositoryFailureDuringUpload_whenUploadAttachment_thenRethrowException()
            throws IOException {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(file.getOriginalFilename())
                .willReturn("lesfiche.pdf");
        given(file.getContentType())
                .willReturn("application/pdf");
        given(file.getSize())
                .willReturn(100L);
        given(file.getBytes())
                .willReturn(FILE_DATA);
        given(lessonTipAttachmentRepository.existsByLessonTipIdAndOriginalFileNameIgnoreCaseAndFileSize(
                LESSON_TIP_ID,
                "lesfiche.pdf",
                100L
        )).willReturn(false);
        given(lessonTipAttachmentRepository.save(any(LessonTipAttachment.class)))
                .willThrow(new RuntimeException("Database error"));

        // Act
        assertThrows(
                RuntimeException.class,
                () -> lessonTipAttachmentService.uploadAttachment(LESSON_TIP_ID, file, authentication)
        );

        // Assert
        then(lessonTipAttachmentRepository).should().save(any(LessonTipAttachment.class));
    }

    @Test
    void givenDuplicateAttachment_whenUploadAttachment_thenThrowIllegalArgumentException() {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");

        given(lessonTipRepository.findById(LESSON_TIP_ID))
                .willReturn(Optional.of(lessonTip));
        given(file.getOriginalFilename())
                .willReturn("lesfiche.pdf");
        given(file.getSize())
                .willReturn(2048L);
        given(lessonTipAttachmentRepository.existsByLessonTipIdAndOriginalFileNameIgnoreCaseAndFileSize(
                LESSON_TIP_ID,
                "lesfiche.pdf",
                2048L
        )).willReturn(true);

        // Act
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonTipAttachmentService.uploadAttachment(LESSON_TIP_ID, file, authentication)
        );

        // Assert
        then(lessonTipAttachmentRepository).should(never()).save(any());
    }

    @Test
    void givenExistingAttachmentId_whenGetAttachmentDownloadData_thenReturnDownloadData()
            throws Exception {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment attachment = createAttachment(
                ATTACHMENT_ID,
                lessonTip,
                "lesfiche.pdf"
        );

        given(lessonTipAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID))
                .willReturn(Optional.of(attachment));

        // Act
        LessonTipAttachmentDownloadDataDto result =
                lessonTipAttachmentService.getAttachmentDownloadData(LESSON_TIP_ID, ATTACHMENT_ID);

        // Assert
        assertThat(result.resource()).isInstanceOf(ByteArrayResource.class);
        assertThat(result.resource().getContentAsByteArray()).isEqualTo(FILE_DATA);
        assertThat(result.originalFileName()).isEqualTo("lesfiche.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void givenExistingAttachmentWithoutFileData_whenGetAttachmentDownloadData_thenThrowResourceNotFoundException() {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment attachment = createAttachment(
                ATTACHMENT_ID,
                lessonTip,
                "lesfiche.pdf"
        );
        attachment.setFileData(null);

        given(lessonTipAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID))
                .willReturn(Optional.of(attachment));

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> lessonTipAttachmentService.getAttachmentDownloadData(LESSON_TIP_ID, ATTACHMENT_ID)
        );

        // Assert
        then(lessonTipAttachmentRepository).should()
                .findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID);
    }

    @Test
    void givenUnknownAttachmentId_whenGetAttachmentDownloadData_thenThrowResourceNotFoundException() {
        // Arrange
        given(lessonTipAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID))
                .willReturn(Optional.empty());

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> lessonTipAttachmentService.getAttachmentDownloadData(LESSON_TIP_ID, ATTACHMENT_ID)
        );

        // Assert
        then(lessonTipAttachmentRepository).should()
                .findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID);
    }

    @Test
    void givenExistingAttachmentAndAuthorizedUser_whenDeleteAttachment_thenDeleteAttachment() {
        // Arrange
        LessonTip lessonTip = createLessonTip(LESSON_TIP_ID, "teacher-123");
        LessonTipAttachment attachment = createAttachment(
                ATTACHMENT_ID,
                lessonTip,
                "lesfiche.pdf"
        );

        given(lessonTipAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID))
                .willReturn(Optional.of(attachment));

        // Act
        lessonTipAttachmentService.deleteAttachment(LESSON_TIP_ID, ATTACHMENT_ID, authentication);

        // Assert
        then(lessonTipAuthorizationService).should().assertCanModify(lessonTip, authentication);
        then(lessonTipAttachmentRepository).should().delete(attachment);
    }

    @Test
    void givenUnknownAttachmentId_whenDeleteAttachment_thenThrowResourceNotFoundException() {
        // Arrange
        given(lessonTipAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, LESSON_TIP_ID))
                .willReturn(Optional.empty());

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> lessonTipAttachmentService.deleteAttachment(LESSON_TIP_ID, ATTACHMENT_ID, authentication)
        );

        // Assert
        then(lessonTipAttachmentRepository).should(never()).delete(any());
    }

    private LessonTip createLessonTip(Long id, String createdByUserId) {
        LessonTip lessonTip = new LessonTip();
        lessonTip.setId(id);
        lessonTip.setTitle("Titel");
        lessonTip.setDescription("Beschrijving");
        lessonTip.setLessonTipType(edu.ap.bib_backend.jpa.enums.LessonTipType.WORKSHEET);
        lessonTip.setCreatedByUserId(createdByUserId);
        lessonTip.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        lessonTip.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 11, 0));
        return lessonTip;
    }

    private LessonTipAttachment createAttachment(
            Long id,
            LessonTip lessonTip,
            String originalFileName
    ) {
        LessonTipAttachment attachment = new LessonTipAttachment();
        attachment.setId(id);
        attachment.setLessonTip(lessonTip);
        attachment.setOriginalFileName(originalFileName);
        attachment.setContentType("application/pdf");
        attachment.setFileSize(1024L);
        attachment.setFileData(FILE_DATA);
        attachment.setUploadedAt(LocalDateTime.of(2026, 4, 21, 14, 15));
        return attachment;
    }

    private LessonTipAttachmentResponseDto createAttachmentResponseDto(Long id, String fileName) {
        return new LessonTipAttachmentResponseDto(
                id,
                fileName,
                "application/pdf",
                1024L,
                LocalDateTime.of(2026, 4, 21, 14, 15)
        );
    }
}