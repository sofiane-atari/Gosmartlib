package edu.ap.bib_backend.service.didactic;


import edu.ap.bib_backend.dto.didactic.CreateDidacticMaterialRequestDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.didactic.DidacticMaterialRepository;
import edu.ap.bib_backend.mapper.didactic.DidacticMaterialMapper;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DidacticMaterialService {

    private final DidacticMaterialRepository didacticMaterialRepository;
    private final DidacticMaterialBookLinkService didacticMaterialBookLinkService;
    private final DidacticMaterialMapper didacticMaterialMapper;
    private final DidacticMaterialCreatorRoleService didacticMaterialCreatorRoleService;
    private final DidacticMaterialAuthorizationService didacticMaterialAuthorizationService;


    public DidacticMaterialService(
            DidacticMaterialRepository didacticMaterialRepository,
            DidacticMaterialBookLinkService didacticMaterialBookLinkService,
            DidacticMaterialMapper didacticMaterialMapper,
            DidacticMaterialCreatorRoleService didacticMaterialCreatorRoleService,
            DidacticMaterialAuthorizationService didacticMaterialAuthorizationService

    ) {
        this.didacticMaterialRepository = didacticMaterialRepository;
        this.didacticMaterialBookLinkService = didacticMaterialBookLinkService;
        this.didacticMaterialMapper = didacticMaterialMapper;
        this.didacticMaterialCreatorRoleService = didacticMaterialCreatorRoleService;
        this.didacticMaterialAuthorizationService = didacticMaterialAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<DidacticMaterialResponseDto> getAllMaterials() {
        return didacticMaterialRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DidacticMaterialResponseDto getMaterialById(Long id) {
        DidacticMaterial material = findMaterialOrThrow(id);
        return toResponse(material);
    }

    public DidacticMaterialResponseDto createMaterial(CreateDidacticMaterialRequestDto request, String currentUserId) {
        DidacticMaterial material = new DidacticMaterial();
        material.setCreatedByUserId(currentUserId); 
        applyRequestFields(material, request);

        DidacticMaterial savedMaterial = didacticMaterialRepository.save(material);
        return toResponse(savedMaterial);
    }

    public DidacticMaterialResponseDto updateMaterial(
            Long id,
            CreateDidacticMaterialRequestDto request,
            Authentication authentication
    ) {
        DidacticMaterial material = findMaterialOrThrow(id);
        didacticMaterialAuthorizationService.assertCanModify(material, authentication);
        applyRequestFields(material, request);

        DidacticMaterial savedMaterial = didacticMaterialRepository.save(material);
        return toResponse(savedMaterial);
    }

    public void deleteMaterial(Long id, Authentication authentication) {
        DidacticMaterial material = findMaterialOrThrow(id);
        didacticMaterialAuthorizationService.assertCanModify(material, authentication);
        didacticMaterialRepository.delete(material);
    }

    private void applyRequestFields(DidacticMaterial material, CreateDidacticMaterialRequestDto request) {
        material.setTitle(request.title().trim());
        material.setDescription(request.description().trim());
        material.setMaterialType(request.materialType());
        material.setSubject(normalizeNullableValue(request.subject()));
        material.setEducationLevel(normalizeNullableValue(request.educationLevel()));
        material.setKeywords(normalizeNullableValue(request.keywords()));
        didacticMaterialBookLinkService.replaceBookLinks(material, request.linkedBookIds());
    }

    private DidacticMaterialResponseDto toResponse(DidacticMaterial material) {
        Role createdByRole = didacticMaterialCreatorRoleService.getCreatorRole(material.getCreatedByUserId());
        return didacticMaterialMapper.toResponse(material, createdByRole);
    }

    private DidacticMaterial findMaterialOrThrow(Long id) {
        return didacticMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Didactisch materiaal niet gevonden met id: " + id
                ));
    }


    private String normalizeNullableValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isBlank() ? null : trimmedValue;
    }

}