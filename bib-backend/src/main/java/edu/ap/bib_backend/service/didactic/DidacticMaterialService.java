package edu.ap.bib_backend.service.didactic;


import edu.ap.bib_backend.dto.didactic.CreateDidacticMaterialRequestDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.repository.didactic.DidacticMaterialRepository;
import edu.ap.bib_backend.mapper.DidacticMaterialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DidacticMaterialService {

    private final DidacticMaterialRepository didacticMaterialRepository;
    private final DidacticMaterialBookLinkService didacticMaterialBookLinkService;
    private final DidacticMaterialMapper didacticMaterialMapper;

    public DidacticMaterialService(
            DidacticMaterialRepository didacticMaterialRepository,
            DidacticMaterialBookLinkService didacticMaterialBookLinkService,
            DidacticMaterialMapper didacticMaterialMapper
    ) {
        this.didacticMaterialRepository = didacticMaterialRepository;
        this.didacticMaterialBookLinkService = didacticMaterialBookLinkService;
        this.didacticMaterialMapper = didacticMaterialMapper;
    }

    @Transactional(readOnly = true)
    public List<DidacticMaterialResponseDto> getAllMaterials() {
        return didacticMaterialMapper.toResponseList(
            didacticMaterialRepository.findAllByOrderByUpdatedAtDesc()
        );
    }

    @Transactional(readOnly = true)
    public DidacticMaterialResponseDto getMaterialById(Long id) {
        DidacticMaterial material = findMaterialOrThrow(id);
        return didacticMaterialMapper.toResponse(material);
    }

    public DidacticMaterialResponseDto createMaterial(CreateDidacticMaterialRequestDto request, String currentUserId) {
        DidacticMaterial material = new DidacticMaterial();
        material.setCreatedByUserId(currentUserId); 
        applyRequestFields(material, request);

        return didacticMaterialMapper.toResponse(didacticMaterialRepository.save(material));
    }

    public DidacticMaterialResponseDto updateMaterial(Long id, CreateDidacticMaterialRequestDto request) {
        DidacticMaterial material = findMaterialOrThrow(id);
        applyRequestFields(material, request);

        return didacticMaterialMapper.toResponse(didacticMaterialRepository.save(material));
    }

    public void deleteMaterial(Long id) {
        DidacticMaterial material = findMaterialOrThrow(id);
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