package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Label;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.dto.LabelStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
import edu.ap.bib_backend.mapper.LabelMapper;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.projection.LabelProjection;

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
            throw new ResourceAlreadyExistsException("name", labelDTO.name(),
                    "Label met de naam '" + labelDTO.name() + "' bestaat al.");
        }

        Label label = labelMapper.toEntity(labelDTO);
        labelRepository.save(label);
        return labelMapper.toDto(label);
    }

    public PageDTO<LabelDTO> getPage(int page, int pageSize, LabelGetOptions options) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("name").ascending());

        Page<? extends LabelProjection> labels;

        if (options.isIncludeBookCount() || options.isIncludeBookCount() && options.getBookCount() != null) {
            labels = labelRepository.findAllFiltered(
                    options.getSearch(),
                    options.getBookCount(),
                    pageable);
        } else if (!options.getSearch().isBlank()) {
            labels = labelRepository.searchByNamerOrDescriptionOrColor(options.getSearch(), pageable);
        } else {
            labels = labelRepository.findAll(pageable);
        }

        List<LabelDTO> data = labels.getContent().stream().map(labelMapper::toDto).toList();
        PaginatedMeta meta = PageMapper.toPaginatedMeta(labels);

        return new PageDTO<LabelDTO>(data, meta);
    }

    public LabelStatsDTO getAllLabelStats() {
        var labels = labelRepository.findAllIncludeBookCount(null).getContent();

        int unActiveCount = labels.stream().filter(l -> l.getBookCount() <= 0).toList().size();
        LabelStatsDTO stats = new LabelStatsDTO(labels.size(), unActiveCount);

        return stats;
    }
}