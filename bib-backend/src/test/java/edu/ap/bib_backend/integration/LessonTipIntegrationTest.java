package edu.ap.bib_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.config.JwtAuthenticationFilter;
import edu.ap.bib_backend.controller.lessontips.LessonTipController;
import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.exception.GlobalExceptionHandler;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.enums.LessonTipType;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipMapper;
import edu.ap.bib_backend.mapper.lessontips.LessonTipMapperImpl;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.lessontips.LessonTipAuthorizationService;
import edu.ap.bib_backend.service.lessontips.LessonTipBookLinkService;
import edu.ap.bib_backend.service.lessontips.LessonTipCreatorRoleService;
import edu.ap.bib_backend.service.lessontips.LessonTipService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LessonTipIntegrationTest.TestApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class LessonTipIntegrationTest {

    private static final String BASE_URL = "/lesson-tips";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LessonTipRepository didacticMaterialRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @BeforeEach
    void setUp() {
        didacticMaterialRepository.deleteAll();
    }


    @Test
    void givenInvalidRequest_whenCreateMaterial_thenReturnBadRequestWithValidationDetails() throws Exception {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "",
                "",
                null,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of()
        );

        // Act
        var result = mockMvc.perform(post(BASE_URL)
                .with(authentication(authenticationToken("teacher-123", "TEACHER")))
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(400))
                .andExpect(jsonPath("$.error.message").value("Validatie mislukt"))
                .andExpect(jsonPath("$.error.details.length()").value(3));

        assertThat(didacticMaterialRepository.findAll()).isEmpty();
    }

    @Test
    void givenExistingMaterials_whenGetAllMaterials_thenReturnOkWithMaterials() throws Exception {
        // Arrange
        didacticMaterialRepository.save(createMaterial("Werkblad 1", "teacher-1"));
        didacticMaterialRepository.save(createMaterial("Werkblad 2", "teacher-2"));

        // Act
        var result = mockMvc.perform(get(BASE_URL)
                .with(authentication(authenticationToken("teacher-123", "TEACHER"))));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void givenExistingMaterialId_whenGetMaterialById_thenReturnOkWithMaterial() throws Exception {
        // Arrange
        LessonTip savedMaterial = didacticMaterialRepository.save(createMaterial("Leesfiche", "teacher-123"));

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{id}", savedMaterial.getId())
                .with(authentication(authenticationToken("teacher-123", "TEACHER"))));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedMaterial.getId()))
                .andExpect(jsonPath("$.title").value("Leesfiche"))
                .andExpect(jsonPath("$.createdByUserId").value("teacher-123"));
    }

    @Test
    void givenUnknownMaterialId_whenGetMaterialById_thenReturnNotFound() throws Exception {
        // Arrange
        Long unknownId = 999L;

        // Act
        var result = mockMvc.perform(get(BASE_URL + "/{id}", unknownId)
                .with(authentication(authenticationToken("teacher-123", "TEACHER"))));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(404))
                .andExpect(jsonPath("$.error.message").value("Item niet gevonden"))
                .andExpect(jsonPath("$.error.details[0].field").value("id"));
    }


    @Test
    void givenUnknownMaterialId_whenDeleteMaterial_thenReturnNotFound() throws Exception {
        // Arrange
        Long unknownId = 999L;

        // Act
        var result = mockMvc.perform(delete(BASE_URL + "/{id}", unknownId)
                .with(authentication(authenticationToken("teacher-123", "TEACHER"))));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(404))
                .andExpect(jsonPath("$.error.message").value("Item niet gevonden"))
                .andExpect(jsonPath("$.error.details[0].field").value("id"));
    }

    private LessonTip createMaterial(String title, String createdByUserId) {
        LessonTip material = new LessonTip();
        material.setTitle(title);
        material.setDescription("Beschrijving");
        material.setLessonTipType(LessonTipType.WORKSHEET);
        material.setSubject("Nederlands");
        material.setEducationLevel("Eerste graad");
        material.setKeywords("lezen");
        material.setCreatedByUserId(createdByUserId);
        return material;
    }

    private UsernamePasswordAuthenticationToken authenticationToken(String userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            OAuth2ClientAutoConfiguration.class,
            OAuth2ClientWebSecurityAutoConfiguration.class,
            OAuth2ResourceServerAutoConfiguration.class,
            Saml2RelyingPartyAutoConfiguration.class
    })
    @EntityScan(basePackages = "edu.ap.bib_backend.jpa.entity")
    @EnableJpaRepositories(basePackages = "edu.ap.bib_backend.jpa.repository")
    @Import({
            GlobalExceptionHandler.class,
            LessonTipController.class,
            LessonTipService.class,
            LessonTipBookLinkService.class,
            LessonTipCreatorRoleService.class,
            LessonTipAuthorizationService.class,
            CurrentUserService.class,
            LessonTipMapperImpl.class
    })
    static class TestApplication {
    }
}