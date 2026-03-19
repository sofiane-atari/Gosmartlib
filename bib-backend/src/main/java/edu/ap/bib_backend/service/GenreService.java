package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Genre;

import edu.ap.bib_backend.jpa.repository.ReviewRepository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.mapper.GenreMapper;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.projection.GenreProjection;

@Service
public class GenreService {
    private final ReviewRepository reviewRepository;
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public GenreService(GenreRepository labelRepository, GenreMapper genreMapper, ReviewRepository reviewRepository) {
        this.genreRepository = labelRepository;
        this.genreMapper = genreMapper;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public GenreDTO createLabel(GenreDTO labelDTO) {
        if (genreRepository.existsById(labelDTO.name())) {
            throw new ResourceAlreadyExistsException("name", labelDTO.name(),
                    "Label met de naam '" + labelDTO.name() + "' bestaat al.");
        }

        Genre label = genreMapper.toEntity(labelDTO);
        genreRepository.save(label);
        return genreMapper.toDto(label);
    }

    public PageDTO<GenreDTO> getPage(int page, int pageSize, LabelGetOptions options) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("name").ascending());

        Page<? extends GenreProjection> labels;

        if (options.isIncludeBookCount() || options.getBookCount() != null) {
            labels = genreRepository.findAllFiltered(
                    options.getSearch(),
                    options.getBookCount(),
                    pageable);
        } else if (!options.getSearch().isBlank()) {
            labels = genreRepository.searchByNamerOrDescriptionOrColor(options.getSearch(), pageable);
        } else {
            labels = genreRepository.findAll(pageable);
        }

        List<GenreDTO> data = labels.getContent().stream().map(genreMapper::toDto).toList();
        PaginatedMeta meta = PageMapper.toPaginatedMeta(labels);

        return new PageDTO<GenreDTO>(data, meta);
    }

    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(genreMapper::toDto)
                .toList();
    }

    public GenreStatsDTO getAllGenreStats() {
        var labels = genreRepository.findAllIncludeBookCount(null).getContent();

        int unActiveCount = labels.stream().filter(l -> l.getBookCount() <= 0).toList().size();
        GenreStatsDTO stats = new GenreStatsDTO(labels.size(), unActiveCount);

        return stats;
    }

    @Transactional
    public void deleteById(String name) {
        Genre label = genreRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(name,
                        "Deze label bestaat niet en kan niet verwijderd worden"));

        genreRepository.deleteById(label.getName());
    }

    @Transactional
    public GenreDTO update(String name, GenreDTO inputDTO) {

        Genre label = genreRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(name,
                        "Deze label bestaat niet en kan niet gewijzigd worden"));

        label.setColor(inputDTO.color());
        label.setDescription(inputDTO.description());

        Genre saved = genreRepository.save(label);
        GenreDTO outputDTO = genreMapper.toDto(saved);

        return outputDTO;
    }
}