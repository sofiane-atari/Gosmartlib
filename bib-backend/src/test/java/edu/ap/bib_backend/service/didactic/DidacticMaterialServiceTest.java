package edu.ap.bib_backend.service.didactic;

import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipBookSummaryResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.enums.LessonTipType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipMapper;
import edu.ap.bib_backend.service.lessontips.LessonTipAuthorizationService;
import edu.ap.bib_backend.service.lessontips.LessonTipBookLinkService;
import edu.ap.bib_backend.service.lessontips.LessonTipCreatorRoleService;
import edu.ap.bib_backend.service.lessontips.LessonTipService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DidacticMaterialServiceTest {

    private static final Long MATERIAL_ID = 7L;
    private static final String CURRENT_USER_ID = "teacher-123";

    @Mock
    private LessonTipRepository didacticMaterialRepository;

    @Mock
    private LessonTipBookLinkService didacticMaterialBookLinkService;

    @Mock
    private LessonTipMapper didacticMaterialMapper;

    @Mock
    private LessonTipCreatorRoleService didacticMaterialCreatorRoleService;

    @Mock
    private LessonTipAuthorizationService didacticMaterialAuthorizationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LessonTipService didacticMaterialService;

    @Test
    void givenExistingMaterials_whenGetAllMaterials_thenReturnMappedResponses() {
        // Arrange
        LessonTip firstMaterial = createMaterial(1L, "teacher-1");
        LessonTip secondMaterial = createMaterial(2L, "teacher-2");

        LessonTipResponseDto firstResponse = createResponseDto(1L, "Materiaal 1");
        LessonTipResponseDto secondResponse = createResponseDto(2L, "Materiaal 2");

        given(didacticMaterialRepository.findAllByOrderByUpdatedAtDesc())
                .willReturn(List.of(firstMaterial, secondMaterial));
        given(didacticMaterialCreatorRoleService.getCreatorRole("teacher-1"))
                .willReturn(Role.TEACHER);
        given(didacticMaterialCreatorRoleService.getCreatorRole("teacher-2"))
                .willReturn(Role.LIBRARIAN);
        given(didacticMaterialMapper.toResponse(firstMaterial, Role.TEACHER))
                .willReturn(firstResponse);
        given(didacticMaterialMapper.toResponse(secondMaterial, Role.LIBRARIAN))
                .willReturn(secondResponse);

        // Act
        List<LessonTipResponseDto> result = didacticMaterialService.getAllLessonTips();

        // Assert
        assertThat(result).containsExactly(firstResponse, secondResponse);

        then(didacticMaterialRepository).should().findAllByOrderByUpdatedAtDesc();
        then(didacticMaterialCreatorRoleService).should().getCreatorRole("teacher-1");
        then(didacticMaterialCreatorRoleService).should().getCreatorRole("teacher-2");
        then(didacticMaterialMapper).should().toResponse(firstMaterial, Role.TEACHER);
        then(didacticMaterialMapper).should().toResponse(secondMaterial, Role.LIBRARIAN);
    }

    @Test
    void givenExistingMaterialId_whenGetMaterialById_thenReturnMappedResponse() {
        // Arrange
        LessonTip material = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        LessonTipResponseDto responseDto = createResponseDto(MATERIAL_ID, "Leesfiche");

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(material));
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(material, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        LessonTipResponseDto result = didacticMaterialService.getLessonTipById(MATERIAL_ID);

        // Assert
        assertThat(result).isEqualTo(responseDto);

        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialCreatorRoleService).should().getCreatorRole(CURRENT_USER_ID);
        then(didacticMaterialMapper).should().toResponse(material, Role.TEACHER);
    }

    @Test
    void givenUnknownMaterialId_whenGetMaterialById_thenThrowResourceNotFoundException() {
        // Arrange
        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> didacticMaterialService.getLessonTipById(MATERIAL_ID)
        );

        // Assert
        assertThat(exception.getFieldName()).isEqualTo("id");
        assertThat(exception.getMessage()).contains("Didactisch materiaal niet gevonden met id: " + MATERIAL_ID);

        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialMapper).shouldHaveNoInteractions();
    }

    @Test
    void givenValidRequest_whenCreateMaterial_thenSaveNormalizedMaterialAndReturnMappedResponse() {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "  Leesfiche  ",
                "  Beschrijving van het materiaal  ",
                LessonTipType.WORKSHEET,
                "  Nederlands  ",
                "   ",
                "   ",
                List.of(100L, 200L)
        );

        LessonTip savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        LessonTipResponseDto responseDto = createResponseDto(MATERIAL_ID, "Leesfiche");

        ArgumentCaptor<LessonTip> materialCaptor = ArgumentCaptor.forClass(LessonTip.class);

        given(didacticMaterialRepository.save(any(LessonTip.class)))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        LessonTipResponseDto result = didacticMaterialService.createLessonTip(requestDto, CURRENT_USER_ID);

        // Assert
        assertThat(result).isEqualTo(responseDto);

        then(didacticMaterialBookLinkService).should()
                .replaceBookLinks(any(LessonTip.class), same(requestDto.linkedBookIds()));
        then(didacticMaterialRepository).should().save(materialCaptor.capture());

        LessonTip capturedMaterial = materialCaptor.getValue();
        assertThat(capturedMaterial.getCreatedByUserId()).isEqualTo(CURRENT_USER_ID);
        assertThat(capturedMaterial.getTitle()).isEqualTo("Leesfiche");
        assertThat(capturedMaterial.getDescription()).isEqualTo("Beschrijving van het materiaal");
        assertThat(capturedMaterial.getLessonTipType()).isEqualTo(LessonTipType.WORKSHEET);
        assertThat(capturedMaterial.getSubject()).isEqualTo("Nederlands");
        assertThat(capturedMaterial.getEducationLevel()).isNull();
        assertThat(capturedMaterial.getKeywords()).isNull();
    }

    @Test
    void givenNullOptionalFields_whenCreateMaterial_thenKeepNullableFieldsAsNull() {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "Titel",
                "Beschrijving",
                LessonTipType.WORKSHEET,
                null,
                null,
                null,
                List.of()
        );

        LessonTip savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        LessonTipResponseDto responseDto = createResponseDto(MATERIAL_ID, "Titel");

        ArgumentCaptor<LessonTip> materialCaptor = ArgumentCaptor.forClass(LessonTip.class);

        given(didacticMaterialRepository.save(any(LessonTip.class)))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        didacticMaterialService.createLessonTip(requestDto, CURRENT_USER_ID);

        // Assert
        then(didacticMaterialRepository).should().save(materialCaptor.capture());

        LessonTip capturedMaterial = materialCaptor.getValue();
        assertThat(capturedMaterial.getSubject()).isNull();
        assertThat(capturedMaterial.getEducationLevel()).isNull();
        assertThat(capturedMaterial.getKeywords()).isNull();
    }

    @Test
    void givenExistingMaterialAndAuthorizedUser_whenUpdateMaterial_thenSaveUpdatedNormalizedMaterialAndReturnMappedResponse() {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "  Bijgewerkte titel  ",
                "  Bijgewerkte beschrijving  ",
                LessonTipType.WORKSHEET,
                "  Geschiedenis  ",
                "  Tweede graad  ",
                "  oorlog, bronnen  ",
                List.of(300L, 400L)
        );

        LessonTip existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        LessonTip savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        LessonTipResponseDto responseDto = createResponseDto(MATERIAL_ID, "Bijgewerkte titel");

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));
        given(didacticMaterialRepository.save(existingMaterial))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        LessonTipResponseDto result =
                didacticMaterialService.updateLessonTip(MATERIAL_ID, requestDto, authentication);

        // Assert
        assertThat(result).isEqualTo(responseDto);
        assertThat(existingMaterial.getTitle()).isEqualTo("Bijgewerkte titel");
        assertThat(existingMaterial.getDescription()).isEqualTo("Bijgewerkte beschrijving");
        assertThat(existingMaterial.getSubject()).isEqualTo("Geschiedenis");
        assertThat(existingMaterial.getEducationLevel()).isEqualTo("Tweede graad");
        assertThat(existingMaterial.getKeywords()).isEqualTo("oorlog, bronnen");

        then(didacticMaterialAuthorizationService).should().assertCanModify(existingMaterial, authentication);
        then(didacticMaterialBookLinkService).should().replaceBookLinks(existingMaterial, requestDto.linkedBookIds());
        then(didacticMaterialRepository).should().save(existingMaterial);
    }

    @Test
    void givenUnknownMaterialId_whenUpdateMaterial_thenThrowResourceNotFoundException() {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "Titel",
                "Beschrijving",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L)
        );

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.empty());

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> didacticMaterialService.updateLessonTip(MATERIAL_ID, requestDto, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialAuthorizationService).shouldHaveNoInteractions();
        then(didacticMaterialRepository).should(never()).save(any());
    }

    @Test
    void givenUnauthorizedUser_whenUpdateMaterial_thenPropagateAccessDeniedExceptionAndDoNotSave() {
        // Arrange
        CreateLessonTipRequestDto requestDto = new CreateLessonTipRequestDto(
                "Titel",
                "Beschrijving",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L)
        );

        LessonTip existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("Geen toestemming."))
                .given(didacticMaterialAuthorizationService)
                .assertCanModify(existingMaterial, authentication);

        // Act
        assertThrows(
                AccessDeniedException.class,
                () -> didacticMaterialService.updateLessonTip(MATERIAL_ID, requestDto, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialRepository).should(never()).save(any());
    }

    @Test
    void givenExistingMaterialAndAuthorizedUser_whenDeleteMaterial_thenDeleteMaterial() {
        // Arrange
        LessonTip existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));

        // Act
        didacticMaterialService.deleteLessonTip(MATERIAL_ID, authentication);

        // Assert
        then(didacticMaterialAuthorizationService).should().assertCanModify(existingMaterial, authentication);
        then(didacticMaterialRepository).should().delete(existingMaterial);
    }

    @Test
    void givenUnknownMaterialId_whenDeleteMaterial_thenThrowResourceNotFoundException() {
        // Arrange
        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.empty());

        // Act
        assertThrows(
                ResourceNotFoundException.class,
                () -> didacticMaterialService.deleteLessonTip(MATERIAL_ID, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialAuthorizationService).shouldHaveNoInteractions();
        then(didacticMaterialRepository).should(never()).delete(any());
    }

    private LessonTip createMaterial(Long id, String createdByUserId) {
        LessonTip material = new LessonTip();
        material.setId(id);
        material.setTitle("Titel");
        material.setDescription("Beschrijving");
        material.setLessonTipType(LessonTipType.WORKSHEET);
        material.setSubject("Nederlands");
        material.setEducationLevel("Eerste graad");
        material.setKeywords("lezen");
        material.setCreatedByUserId(createdByUserId);
        material.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        material.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 11, 0));
        return material;
    }

    private LessonTipResponseDto createResponseDto(Long id, String title) {
        return new LessonTipResponseDto(
                id,
                title,
                "Beschrijving",
                LessonTipType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                CURRENT_USER_ID,
                Role.TEACHER,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0),
                List.of(new LessonTipBookSummaryResponseDto(
                        100L,
                        "Matilda",
                        "Roald Dahl",
                        "https://example.com/matilda.jpg"
                ))
        );
    }
}