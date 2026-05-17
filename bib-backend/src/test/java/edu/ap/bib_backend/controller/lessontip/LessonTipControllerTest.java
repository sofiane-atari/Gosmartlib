package edu.ap.bib_backend.controller.lessontip;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.config.JwtAuthenticationFilter;
import edu.ap.bib_backend.controller.lessontips.LessonTipController;
import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipBookSummaryResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.enums.LessonTipType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.lessontips.LessonTipService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonTipController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class LessonTipControllerTest {

    private static final String BASE_URL = "/lesson-tips";
    private static final Long MATERIAL_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LessonTipService didacticMaterialService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    void givenExistingMaterials_whenGetAllMaterials_thenReturnOkWithResponseBody() throws Exception {
        // Arrange
        LessonTipResponseDto firstMaterial = createResponseDto(1L, "Werkblad 1");
        LessonTipResponseDto secondMaterial = createResponseDto(2L, "Werkblad 2");

        given(didacticMaterialService.getAllLessonTips(null))
                .willReturn(List.of(firstMaterial, secondMaterial));

        // Act
        var result = mockMvc.perform(get(BASE_URL));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Werkblad 1"))
                .andExpect(jsonPath("$[0].lessonTipType").value("WORKSHEET"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Werkblad 2"));

        then(didacticMaterialService).should().getAllLessonTips(null);
    }

    @Test
    void givenExistingMaterialId_whenGetMaterialById_thenReturnOkWithResponseBody() throws Exception {
        // Arrange
        LessonTipResponseDto responseDto = createResponseDto(MATERIAL_ID, "Leesfiche");

        given(didacticMaterialService.getLessonTipById(MATERIAL_ID))
                .willReturn(responseDto);

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{id}", MATERIAL_ID));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MATERIAL_ID))
                .andExpect(jsonPath("$.title").value("Leesfiche"))
                .andExpect(jsonPath("$.description").value("Beschrijving van het didactisch materiaal"))
                .andExpect(jsonPath("$.lessonTipType").value("WORKSHEET"))
                .andExpect(jsonPath("$.subject").value("Nederlands"))
                .andExpect(jsonPath("$.createdByUserId").value("teacher-123"))
                .andExpect(jsonPath("$.createdByRole").value("TEACHER"))
                .andExpect(jsonPath("$.linkedBooks.length()").value(1))
                .andExpect(jsonPath("$.linkedBooks[0].id").value(100L))
                .andExpect(jsonPath("$.linkedBooks[0].title").value("Matilda"));

        then(didacticMaterialService).should().getLessonTipById(MATERIAL_ID);
    }

    @Test
    void givenUnknownMaterialId_whenGetMaterialById_thenReturnNotFound() throws Exception {
        // Arrange
        given(didacticMaterialService.getLessonTipById(MATERIAL_ID))
                .willThrow(new ResourceNotFoundException(
                        "id",
                        "Didactisch materiaal niet gevonden met id: " + MATERIAL_ID
                ));

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{id}", MATERIAL_ID));

        // Assert
        result.andExpect(status().isNotFound());

        then(didacticMaterialService).should().getLessonTipById(MATERIAL_ID);
    }

    

    @Test
    void givenBlankTitle_whenCreateMaterial_thenReturnBadRequestAndDoNotCallService() throws Exception {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                " ",
                "Beschrijving",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L, 2L)
        );

        // Act
        var result = mockMvc.perform(post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        // Assert
        result.andExpect(status().isBadRequest());

        then(currentUserService).should(never()).extractCurrentUserId(any(Authentication.class));
        then(didacticMaterialService).should(never()).createLessonTip(any(), any());
    }

    @Test
    void givenMissingMaterialType_whenCreateMaterial_thenReturnBadRequestAndDoNotCallService() throws Exception {
        // Arrange
        String requestBody = """
                {
                  "title": "Leesfiche",
                  "description": "Beschrijving",
                  "subject": "Nederlands",
                  "educationLevel": "Eerste graad",
                  "keywords": "lezen",
                  "linkedBookIds": [1, 2]
                }
                """;

        // Act
        var result = mockMvc.perform(post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isBadRequest());

        then(currentUserService).should(never()).extractCurrentUserId(any(Authentication.class));
        then(didacticMaterialService).should(never()).createLessonTip(any(), any());
    }

    @Test
    void givenTitleLongerThan150Characters_whenCreateMaterial_thenReturnBadRequestAndDoNotCallService() throws Exception {
        // Arrange
        String tooLongTitle = "a".repeat(151);
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                tooLongTitle,
                "Beschrijving",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L)
        );

        // Act
        var result = mockMvc.perform(post(BASE_URL)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        // Assert
        result.andExpect(status().isBadRequest());

        then(currentUserService).should(never()).extractCurrentUserId(any(Authentication.class));
        then(didacticMaterialService).should(never()).createLessonTip(any(), any());
    }

   

    @Test
    void givenInvalidRequestBody_whenUpdateMaterial_thenReturnBadRequestAndDoNotCallService() throws Exception {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "",
                "",
                null,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L)
        );

        // Act
        var result = mockMvc.perform(put(BASE_URL + "/{id}", MATERIAL_ID)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        // Assert
        result.andExpect(status().isBadRequest());

        then(didacticMaterialService).should(never()).updateLessonTip(any(), any(), any());
    }


    private LessonTipResponseDto createResponseDto(Long id, String title) {
        return new LessonTipResponseDto(
                id,
                title,
                "Beschrijving van het didactisch materiaal",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen, taal",
                "teacher-123",
                Role.TEACHER,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 30),
                List.of(new LessonTipBookSummaryResponseDto(
                        100L,
                        "Matilda",
                        "Roald Dahl",
                        "https://example.com/matilda.jpg"
                ))
        );
    }
}