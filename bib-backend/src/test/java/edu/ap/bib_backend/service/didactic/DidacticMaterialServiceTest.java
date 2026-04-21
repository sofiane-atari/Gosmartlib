package edu.ap.bib_backend.service.didactic;

import edu.ap.bib_backend.dto.didactic.CreateDidacticMaterialRequestDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialBookSummaryResponseDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.enums.DidacticMaterialType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.didactic.DidacticMaterialRepository;
import edu.ap.bib_backend.mapper.didactic.DidacticMaterialMapper;
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
    private DidacticMaterialRepository didacticMaterialRepository;

    @Mock
    private DidacticMaterialBookLinkService didacticMaterialBookLinkService;

    @Mock
    private DidacticMaterialMapper didacticMaterialMapper;

    @Mock
    private DidacticMaterialCreatorRoleService didacticMaterialCreatorRoleService;

    @Mock
    private DidacticMaterialAuthorizationService didacticMaterialAuthorizationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DidacticMaterialService didacticMaterialService;

    @Test
    void givenExistingMaterials_whenGetAllMaterials_thenReturnMappedResponses() {
        // Arrange
        DidacticMaterial firstMaterial = createMaterial(1L, "teacher-1");
        DidacticMaterial secondMaterial = createMaterial(2L, "teacher-2");

        DidacticMaterialResponseDto firstResponse = createResponseDto(1L, "Materiaal 1");
        DidacticMaterialResponseDto secondResponse = createResponseDto(2L, "Materiaal 2");

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
        List<DidacticMaterialResponseDto> result = didacticMaterialService.getAllMaterials();

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
        DidacticMaterial material = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        DidacticMaterialResponseDto responseDto = createResponseDto(MATERIAL_ID, "Leesfiche");

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(material));
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(material, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        DidacticMaterialResponseDto result = didacticMaterialService.getMaterialById(MATERIAL_ID);

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
                () -> didacticMaterialService.getMaterialById(MATERIAL_ID)
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
        CreateDidacticMaterialRequestDto requestDto = new CreateDidacticMaterialRequestDto(
                "  Leesfiche  ",
                "  Beschrijving van het materiaal  ",
                DidacticMaterialType.WORKSHEET,
                "  Nederlands  ",
                "   ",
                "   ",
                List.of(100L, 200L)
        );

        DidacticMaterial savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        DidacticMaterialResponseDto responseDto = createResponseDto(MATERIAL_ID, "Leesfiche");

        ArgumentCaptor<DidacticMaterial> materialCaptor = ArgumentCaptor.forClass(DidacticMaterial.class);

        given(didacticMaterialRepository.save(any(DidacticMaterial.class)))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        DidacticMaterialResponseDto result = didacticMaterialService.createMaterial(requestDto, CURRENT_USER_ID);

        // Assert
        assertThat(result).isEqualTo(responseDto);

        then(didacticMaterialBookLinkService).should()
                .replaceBookLinks(any(DidacticMaterial.class), same(requestDto.linkedBookIds()));
        then(didacticMaterialRepository).should().save(materialCaptor.capture());

        DidacticMaterial capturedMaterial = materialCaptor.getValue();
        assertThat(capturedMaterial.getCreatedByUserId()).isEqualTo(CURRENT_USER_ID);
        assertThat(capturedMaterial.getTitle()).isEqualTo("Leesfiche");
        assertThat(capturedMaterial.getDescription()).isEqualTo("Beschrijving van het materiaal");
        assertThat(capturedMaterial.getMaterialType()).isEqualTo(DidacticMaterialType.WORKSHEET);
        assertThat(capturedMaterial.getSubject()).isEqualTo("Nederlands");
        assertThat(capturedMaterial.getEducationLevel()).isNull();
        assertThat(capturedMaterial.getKeywords()).isNull();
    }

    @Test
    void givenNullOptionalFields_whenCreateMaterial_thenKeepNullableFieldsAsNull() {
        // Arrange
        CreateDidacticMaterialRequestDto requestDto = new CreateDidacticMaterialRequestDto(
                "Titel",
                "Beschrijving",
                DidacticMaterialType.WORKSHEET,
                null,
                null,
                null,
                List.of()
        );

        DidacticMaterial savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        DidacticMaterialResponseDto responseDto = createResponseDto(MATERIAL_ID, "Titel");

        ArgumentCaptor<DidacticMaterial> materialCaptor = ArgumentCaptor.forClass(DidacticMaterial.class);

        given(didacticMaterialRepository.save(any(DidacticMaterial.class)))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        didacticMaterialService.createMaterial(requestDto, CURRENT_USER_ID);

        // Assert
        then(didacticMaterialRepository).should().save(materialCaptor.capture());

        DidacticMaterial capturedMaterial = materialCaptor.getValue();
        assertThat(capturedMaterial.getSubject()).isNull();
        assertThat(capturedMaterial.getEducationLevel()).isNull();
        assertThat(capturedMaterial.getKeywords()).isNull();
    }

    @Test
    void givenExistingMaterialAndAuthorizedUser_whenUpdateMaterial_thenSaveUpdatedNormalizedMaterialAndReturnMappedResponse() {
        // Arrange
        CreateDidacticMaterialRequestDto requestDto = new CreateDidacticMaterialRequestDto(
                "  Bijgewerkte titel  ",
                "  Bijgewerkte beschrijving  ",
                DidacticMaterialType.WORKSHEET,
                "  Geschiedenis  ",
                "  Tweede graad  ",
                "  oorlog, bronnen  ",
                List.of(300L, 400L)
        );

        DidacticMaterial existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        DidacticMaterial savedMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);
        DidacticMaterialResponseDto responseDto = createResponseDto(MATERIAL_ID, "Bijgewerkte titel");

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));
        given(didacticMaterialRepository.save(existingMaterial))
                .willReturn(savedMaterial);
        given(didacticMaterialCreatorRoleService.getCreatorRole(CURRENT_USER_ID))
                .willReturn(Role.TEACHER);
        given(didacticMaterialMapper.toResponse(savedMaterial, Role.TEACHER))
                .willReturn(responseDto);

        // Act
        DidacticMaterialResponseDto result =
                didacticMaterialService.updateMaterial(MATERIAL_ID, requestDto, authentication);

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
        CreateDidacticMaterialRequestDto requestDto = new CreateDidacticMaterialRequestDto(
                "Titel",
                "Beschrijving",
                DidacticMaterialType.WORKSHEET,
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
                () -> didacticMaterialService.updateMaterial(MATERIAL_ID, requestDto, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialAuthorizationService).shouldHaveNoInteractions();
        then(didacticMaterialRepository).should(never()).save(any());
    }

    @Test
    void givenUnauthorizedUser_whenUpdateMaterial_thenPropagateAccessDeniedExceptionAndDoNotSave() {
        // Arrange
        CreateDidacticMaterialRequestDto requestDto = new CreateDidacticMaterialRequestDto(
                "Titel",
                "Beschrijving",
                DidacticMaterialType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                List.of(1L)
        );

        DidacticMaterial existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("Geen toestemming."))
                .given(didacticMaterialAuthorizationService)
                .assertCanModify(existingMaterial, authentication);

        // Act
        assertThrows(
                AccessDeniedException.class,
                () -> didacticMaterialService.updateMaterial(MATERIAL_ID, requestDto, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialRepository).should(never()).save(any());
    }

    @Test
    void givenExistingMaterialAndAuthorizedUser_whenDeleteMaterial_thenDeleteMaterial() {
        // Arrange
        DidacticMaterial existingMaterial = createMaterial(MATERIAL_ID, CURRENT_USER_ID);

        given(didacticMaterialRepository.findById(MATERIAL_ID))
                .willReturn(Optional.of(existingMaterial));

        // Act
        didacticMaterialService.deleteMaterial(MATERIAL_ID, authentication);

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
                () -> didacticMaterialService.deleteMaterial(MATERIAL_ID, authentication)
        );

        // Assert
        then(didacticMaterialRepository).should().findById(MATERIAL_ID);
        then(didacticMaterialAuthorizationService).shouldHaveNoInteractions();
        then(didacticMaterialRepository).should(never()).delete(any());
    }

    private DidacticMaterial createMaterial(Long id, String createdByUserId) {
        DidacticMaterial material = new DidacticMaterial();
        material.setId(id);
        material.setTitle("Titel");
        material.setDescription("Beschrijving");
        material.setMaterialType(DidacticMaterialType.WORKSHEET);
        material.setSubject("Nederlands");
        material.setEducationLevel("Eerste graad");
        material.setKeywords("lezen");
        material.setCreatedByUserId(createdByUserId);
        material.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 0));
        material.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 11, 0));
        return material;
    }

    private DidacticMaterialResponseDto createResponseDto(Long id, String title) {
        return new DidacticMaterialResponseDto(
                id,
                title,
                "Beschrijving",
                DidacticMaterialType.WORKSHEET,
                "Nederlands",
                "Eerste graad",
                "lezen",
                CURRENT_USER_ID,
                Role.TEACHER,
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0),
                List.of(new DidacticMaterialBookSummaryResponseDto(
                        100L,
                        "Matilda",
                        "Roald Dahl",
                        "https://example.com/matilda.jpg"
                ))
        );
    }
}