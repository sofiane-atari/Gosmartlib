package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Label;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.jpa.repository.LabelRepository;

@Service
public class LabelService {
    private final LabelRepository labelRepository;

    public LabelService(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    public LabelDTO createLabel(LabelDTO labelDTO) {
        Label label = toLabel(labelDTO);
        labelRepository.save(label);
        return toLabelDTO(label);
    }

    public Label toLabel(LabelDTO labelDTO) {
        Label label = new Label(labelDTO.name(), labelDTO.description(), labelDTO.color());
        return label;
    }

    public LabelDTO toLabelDTO(Label label) {
        return new LabelDTO(
                label.getName(),
                label.getDescription(),
                label.getColor());
    }
}