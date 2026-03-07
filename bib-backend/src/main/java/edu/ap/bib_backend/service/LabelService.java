package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Label;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
import edu.ap.bib_backend.mapper.LabelMapper;

@Service
public class LabelService {
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    public LabelService(LabelRepository labelRepository, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
    }

    public LabelDTO createLabel(LabelDTO labelDTO) {
        if (labelRepository.existsById(labelDTO.name())) {
            throw new ResourceAlreadyExistsException("name", labelDTO.name(), "Label met de naam '" + labelDTO.name() + "' bestaat al.");
        }

        Label label = labelMapper.toEntity(labelDTO);
        labelRepository.save(label);
        return labelMapper.toDto(label);
    }

}