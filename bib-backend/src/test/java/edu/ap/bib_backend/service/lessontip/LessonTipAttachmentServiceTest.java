// package edu.ap.bib_backend.service.lessontip;

// import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
// import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
// import edu.ap.bib_backend.exception.ResourceNotFoundException;
// import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
// import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;
// import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipAttachmentRepository;
// import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
// import edu.ap.bib_backend.mapper.lessontips.LessonTipAttachmentMapper;
// import edu.ap.bib_backend.service.lessontips.LessonTipAttachmentService;
// import edu.ap.bib_backend.service.lessontips.LessonTipAuthorizationService;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.security.core.Authentication;
// import org.springframework.web.multipart.MultipartFile;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.BDDMockito.then;
// import static org.mockito.Mockito.never;

// @ExtendWith(MockitoExtension.class)
// class LessonTipAttachmentServiceTest {

//     private static final Long MATERIAL_ID = 7L;
//     private static final Long ATTACHMENT_ID = 9L;

//     @Mock
//     private LessonTipRepository didacticMaterialRepository;

//     @Mock
//     private LessonTipAttachmentRepository didacticMaterialAttachmentRepository;

//     @Mock
//     private LessonTipAttachmentMapper didacticMaterialAttachmentMapper;

//     @Mock
//     private LessonTipAuthorizationService didacticMaterialAuthorizationService;

//     @Mock
//     private MultipartFile file;

//     @Mock
//     private Authentication authentication;

//     @InjectMocks
//     private LessonTipAttachmentService didacticMaterialAttachmentService;

//     @Test
//     void givenExistingMaterialId_whenGetAttachmentsByMaterialId_thenReturnMappedAttachments() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");
//         LessonTipAttachment firstAttachment = createAttachment(1L, material, "first.pdf", "stored-first.pdf");
//         LessonTipAttachment secondAttachment = createAttachment(2L, material, "second.pdf", "stored-second.pdf");

//         LessonTipAttachmentResponseDto firstResponse = createAttachmentResponseDto(1L, "first.pdf");
//         LessonTipAttachmentResponseDto secondResponse = createAttachmentResponseDto(2L, "second.pdf");

//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.of(material));
//         given(didacticMaterialAttachmentRepository.findAllByLessonTipIdOrderByUploadedAtDesc(MATERIAL_ID))
//                 .willReturn(List.of(firstAttachment, secondAttachment));
//         given(didacticMaterialAttachmentMapper.toResponseList(List.of(firstAttachment, secondAttachment)))
//                 .willReturn(List.of(firstResponse, secondResponse));

//         // Act
//         List<LessonTipAttachmentResponseDto> result =
//                 didacticMaterialAttachmentService.getAttachmentsByLessonTipId(MATERIAL_ID);

//         // Assert
//         assertThat(result).containsExactly(firstResponse, secondResponse);

//         then(didacticMaterialRepository).should().findById(MATERIAL_ID);
//         then(didacticMaterialAttachmentRepository).should()
//                 .findAllByLessonTipIdOrderByUploadedAtDesc(MATERIAL_ID);
//     }

//     @Test
//     void givenUnknownMaterialId_whenGetAttachmentsByMaterialId_thenThrowResourceNotFoundException() {
//         // Arrange
//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.empty());

//         // Act
//         assertThrows(
//                 ResourceNotFoundException.class,
//                 () -> didacticMaterialAttachmentService.getAttachmentsByLessonTipId(MATERIAL_ID)
//         );

//         // Assert
//         then(didacticMaterialRepository).should().findById(MATERIAL_ID);
//         then(didacticMaterialAttachmentRepository).should(never())
//                 .findAllByLessonTipIdOrderByUploadedAtDesc(any());
//     }

//     @Test
//     void givenValidFileAndAuthorizedUser_whenUploadAttachment_thenSaveAttachmentAndReturnMappedResponse() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");
//         LessonTipAttachment savedAttachment = createAttachment(
//                 ATTACHMENT_ID,
//                 material,
//                 "lesfiche.pdf",
//                 "stored-123.pdf"
//         );
//         LessonTipAttachmentResponseDto responseDto = createAttachmentResponseDto(ATTACHMENT_ID, "lesfiche.pdf");

//         ArgumentCaptor<LessonTipAttachment> attachmentCaptor =
//                 ArgumentCaptor.forClass(LessonTipAttachment.class);

//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.of(material));
//         given(fileStorageService.storeFile(file))
//                 .willReturn("stored-123.pdf");
//         given(file.getOriginalFilename())
//                 .willReturn("  lesfiche.pdf  ");
//         given(file.getContentType())
//                 .willReturn("application/pdf");
//         given(file.getSize())
//                 .willReturn(2048L);
//         given(didacticMaterialAttachmentRepository.save(any(LessonTipAttachment.class)))
//                 .willReturn(savedAttachment);
//         given(didacticMaterialAttachmentMapper.toResponse(savedAttachment))
//                 .willReturn(responseDto);

//         // Act
//         LessonTipAttachmentResponseDto result =
//                 didacticMaterialAttachmentService.uploadAttachment(MATERIAL_ID, file, authentication);

//         // Assert
//         assertThat(result).isEqualTo(responseDto);

//         then(didacticMaterialAuthorizationService).should().assertCanModify(material, authentication);
//         then(fileStorageService).should().storeFile(file);
//         then(didacticMaterialAttachmentRepository).should().save(attachmentCaptor.capture());

//         LessonTipAttachment capturedAttachment = attachmentCaptor.getValue();
//         assertThat(capturedAttachment.getLessonTip()).isEqualTo(material);
//         assertThat(capturedAttachment.getOriginalFileName()).isEqualTo("lesfiche.pdf");
//         assertThat(capturedAttachment.getStoredFileName()).isEqualTo("stored-123.pdf");
//         assertThat(capturedAttachment.getContentType()).isEqualTo("application/pdf");
//         assertThat(capturedAttachment.getFileSize()).isEqualTo(2048L);
//     }

//     @Test
//     void givenBlankContentType_whenUploadAttachment_thenFallbackToApplicationOctetStream() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");
//         LessonTipAttachment savedAttachment = createAttachment(
//                 ATTACHMENT_ID,
//                 material,
//                 "bestand.bin",
//                 "stored-123.bin"
//         );

//         ArgumentCaptor<LessonTipAttachment> attachmentCaptor =
//                 ArgumentCaptor.forClass(LessonTipAttachment.class);

//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.of(material));
//         given(fileStorageService.storeFile(file))
//                 .willReturn("stored-123.bin");
//         given(file.getOriginalFilename())
//                 .willReturn("bestand.bin");
//         given(file.getContentType())
//                 .willReturn(" ");
//         given(file.getSize())
//                 .willReturn(50L);
//         given(didacticMaterialAttachmentRepository.save(any(LessonTipAttachment.class)))
//                 .willReturn(savedAttachment);
//         given(didacticMaterialAttachmentMapper.toResponse(savedAttachment))
//                 .willReturn(createAttachmentResponseDto(ATTACHMENT_ID, "bestand.bin"));

//         // Act
//         didacticMaterialAttachmentService.uploadAttachment(MATERIAL_ID, file, authentication);

//         // Assert
//         then(didacticMaterialAttachmentRepository).should().save(attachmentCaptor.capture());
//         assertThat(attachmentCaptor.getValue().getContentType()).isEqualTo("application/octet-stream");
//     }

//     @Test
//     void givenMissingOriginalFileName_whenUploadAttachment_thenDeleteStoredFileAndRethrowException() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");

//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.of(material));
//         given(fileStorageService.storeFile(file))
//                 .willReturn("stored-123.pdf");
//         given(file.getOriginalFilename())
//                 .willReturn(" ");

//         // Act
//         assertThrows(
//                 IllegalArgumentException.class,
//                 () -> didacticMaterialAttachmentService.uploadAttachment(MATERIAL_ID, file, authentication)
//         );

//         // Assert
//         then(fileStorageService).should().deleteFile("stored-123.pdf");
//         then(didacticMaterialAttachmentRepository).should(never()).save(any());
//     }

//     @Test
//     void givenRepositoryFailureDuringUpload_whenUploadAttachment_thenDeleteStoredFileAndRethrowException() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");

//         given(didacticMaterialRepository.findById(MATERIAL_ID))
//                 .willReturn(Optional.of(material));
//         given(fileStorageService.storeFile(file))
//                 .willReturn("stored-123.pdf");
//         given(file.getOriginalFilename())
//                 .willReturn("lesfiche.pdf");
//         given(file.getContentType())
//                 .willReturn("application/pdf");
//         given(file.getSize())
//                 .willReturn(100L);
//         given(didacticMaterialAttachmentRepository.save(any(LessonTipAttachment.class)))
//                 .willThrow(new RuntimeException("Database error"));

//         // Act
//         assertThrows(
//                 RuntimeException.class,
//                 () -> didacticMaterialAttachmentService.uploadAttachment(MATERIAL_ID, file, authentication)
//         );

//         // Assert
//         then(fileStorageService).should().deleteFile("stored-123.pdf");
//     }

//     @Test
//     void givenExistingAttachmentId_whenGetAttachmentDownloadData_thenReturnDownloadData() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");
//         LessonTipAttachment attachment = createAttachment(
//                 ATTACHMENT_ID,
//                 material,
//                 "lesfiche.pdf",
//                 "stored-123.pdf"
//         );
//         ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

//         given(didacticMaterialAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, MATERIAL_ID))
//                 .willReturn(Optional.of(attachment));
//         given(fileStorageService.loadAsResource("stored-123.pdf"))
//                 .willReturn(resource);

//         // Act
//         LessonTipAttachmentDownloadDataDto result =
//                 didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID);

//         // Assert
//         assertThat(result.resource()).isEqualTo(resource);
//         assertThat(result.originalFileName()).isEqualTo("lesfiche.pdf");
//         assertThat(result.contentType()).isEqualTo("application/pdf");

//         then(fileStorageService).should().loadAsResource("stored-123.pdf");
//     }

//     @Test
//     void givenUnknownAttachmentId_whenGetAttachmentDownloadData_thenThrowResourceNotFoundException() {
//         // Arrange
//         given(didacticMaterialAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, MATERIAL_ID))
//                 .willReturn(Optional.empty());

//         // Act
//         assertThrows(
//                 ResourceNotFoundException.class,
//                 () -> didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID)
//         );

//         // Assert
//         then(fileStorageService).should(never()).loadAsResource(any());
//     }

//     @Test
//     void givenExistingAttachmentAndAuthorizedUser_whenDeleteAttachment_thenDeleteAttachmentAndStoredFile() {
//         // Arrange
//         LessonTip material = createMaterial(MATERIAL_ID, "teacher-123");
//         LessonTipAttachment attachment = createAttachment(
//                 ATTACHMENT_ID,
//                 material,
//                 "lesfiche.pdf",
//                 "stored-123.pdf"
//         );

//         given(didacticMaterialAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, MATERIAL_ID))
//                 .willReturn(Optional.of(attachment));

//         // Act
//         didacticMaterialAttachmentService.deleteAttachment(MATERIAL_ID, ATTACHMENT_ID, authentication);

//         // Assert
//         then(didacticMaterialAuthorizationService).should().assertCanModify(material, authentication);
//         then(didacticMaterialAttachmentRepository).should().delete(attachment);
//         then(fileStorageService).should().deleteFile("stored-123.pdf");
//     }

//     @Test
//     void givenUnknownAttachmentId_whenDeleteAttachment_thenThrowResourceNotFoundException() {
//         // Arrange
//         given(didacticMaterialAttachmentRepository.findByIdAndLessonTipId(ATTACHMENT_ID, MATERIAL_ID))
//                 .willReturn(Optional.empty());

//         // Act
//         assertThrows(
//                 ResourceNotFoundException.class,
//                 () -> didacticMaterialAttachmentService.deleteAttachment(MATERIAL_ID, ATTACHMENT_ID, authentication)
//         );

//         // Assert
//         then(didacticMaterialAttachmentRepository).should(never()).delete(any());
//         then(fileStorageService).should(never()).deleteFile(any());
//     }

//     private LessonTip createMaterial(Long id, String createdByUserId) {
//         LessonTip material = new LessonTip();
//         material.setId(id);
//         material.setTitle("Titel");
//         material.setDescription("Beschrijving");
//         material.setLessonTipType(edu.ap.bib_backend.jpa.enums.LessonTipType.WORKSHEET);
//         material.setCreatedByUserId(createdByUserId);
//         material.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 0));
//         material.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 11, 0));
//         return material;
//     }

//     private LessonTipAttachment createAttachment(
//             Long id,
//             LessonTip material,
//             String originalFileName,
//             String storedFileName
//     ) {
//         LessonTipAttachment attachment = new LessonTipAttachment();
//         attachment.setId(id);
//         attachment.setLessonTip(material);
//         attachment.setOriginalFileName(originalFileName);
//         attachment.setStoredFileName(storedFileName);
//         attachment.setContentType("application/pdf");
//         attachment.setFileSize(1024L);
//         attachment.setUploadedAt(LocalDateTime.of(2026, 4, 21, 14, 15));
//         return attachment;
//     }

//     private LessonTipAttachmentResponseDto createAttachmentResponseDto(Long id, String fileName) {
//         return new LessonTipAttachmentResponseDto(
//                 id,
//                 fileName,
//                 "application/pdf",
//                 1024L,
//                 LocalDateTime.of(2026, 4, 21, 14, 15)
//         );
//     }
// }