package edu.ap.bib_backend.controller.didactic;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.config.JwtAuthenticationFilter;
import edu.ap.bib_backend.controller.lessontips.LessonTipAttachmentController;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentDownloadDataDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.lessontips.LessonTipAttachmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonTipAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class DidacticMaterialAttachmentControllerTest {

    private static final String BASE_URL = "/didactic-materials/{didacticMaterialId}/attachments";
    private static final Long MATERIAL_ID = 7L;
    private static final Long ATTACHMENT_ID = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonTipAttachmentService didacticMaterialAttachmentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    void givenExistingAttachments_whenGetAttachments_thenReturnOkWithResponseBody() throws Exception {
        // Arrange
        LessonTipAttachmentResponseDto firstAttachment = createAttachmentResponseDto(1L, "lesfiche.pdf");
        LessonTipAttachmentResponseDto secondAttachment = createAttachmentResponseDto(2L, "werkblad.docx");

        given(didacticMaterialAttachmentService.getAttachmentsByLessonTipId(MATERIAL_ID))
                .willReturn(List.of(firstAttachment, secondAttachment));

        // Act
        var result = mockMvc.perform(get(BASE_URL, MATERIAL_ID));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].originalFileName").value("lesfiche.pdf"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].originalFileName").value("werkblad.docx"));

        then(didacticMaterialAttachmentService).should().getAttachmentsByLessonTipId(ATTACHMENT_ID);
    }

    @Test
    void givenUnknownMaterialId_whenGetAttachments_thenReturnNotFound() throws Exception {
        // Arrange
        given(didacticMaterialAttachmentService.getAttachmentsByLessonTipId(MATERIAL_ID))
                .willThrow(new ResourceNotFoundException(
                        "id",
                        "Didactisch materiaal niet gevonden met id: " + MATERIAL_ID
                ));

        // Act
        var result = mockMvc.perform(get(BASE_URL, MATERIAL_ID));

        // Assert
        result.andExpect(status().isNotFound());

        then(didacticMaterialAttachmentService).should().getAttachmentsByLessonTipId(MATERIAL_ID);
    }


    @Test
    void givenValidAttachmentId_whenDownloadAttachment_thenReturnOkWithAttachmentHeadersAndContent() throws Exception {
        // Arrange
        ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

        LessonTipAttachmentDownloadDataDto downloadData =
                new LessonTipAttachmentDownloadDataDto(
                        resource,
                        "lesfiche.pdf",
                        "application/pdf"
                );

        given(didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID))
                .willReturn(downloadData);

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{attachmentId}/download", MATERIAL_ID, ATTACHMENT_ID));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("filename=\"lesfiche.pdf\"")))
                .andExpect(content().bytes("pdf-content".getBytes()));

        then(didacticMaterialAttachmentService).should()
                .getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID);
    }

    @Test
    void givenBlankContentType_whenDownloadAttachment_thenReturnOctetStream() throws Exception {
        // Arrange
        ByteArrayResource resource = new ByteArrayResource("file-content".getBytes());

        LessonTipAttachmentDownloadDataDto downloadData =
                new LessonTipAttachmentDownloadDataDto(
                        resource,
                        "bijlage.bin",
                        " "
                );

        given(didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID))
                .willReturn(downloadData);

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{attachmentId}/download", MATERIAL_ID, ATTACHMENT_ID));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition", containsString("filename=\"bijlage.bin\"")));

        then(didacticMaterialAttachmentService).should()
                .getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID);
    }

    @Test
    void givenInvalidContentType_whenDownloadAttachment_thenReturnOctetStream() throws Exception {
        // Arrange
        ByteArrayResource resource = new ByteArrayResource("file-content".getBytes());

        LessonTipAttachmentDownloadDataDto downloadData =
                new LessonTipAttachmentDownloadDataDto(
                        resource,
                        "bijlage.bin",
                        "invalid/content type/with spaces"
                );

        given(didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID))
                .willReturn(downloadData);

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{attachmentId}/download", MATERIAL_ID, ATTACHMENT_ID));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));

        then(didacticMaterialAttachmentService).should()
                .getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID);
    }

    @Test
    void givenUnknownAttachmentId_whenDownloadAttachment_thenReturnNotFound() throws Exception {
        // Arrange
        given(didacticMaterialAttachmentService.getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID))
                .willThrow(new ResourceNotFoundException(
                        "attachmentId",
                        "Bijlage niet gevonden met id: " + ATTACHMENT_ID
                ));

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{attachmentId}/download", MATERIAL_ID, ATTACHMENT_ID));

        // Assert
        result.andExpect(status().isNotFound());

        then(didacticMaterialAttachmentService).should()
                .getAttachmentDownloadData(MATERIAL_ID, ATTACHMENT_ID);
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