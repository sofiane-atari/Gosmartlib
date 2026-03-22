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
import edu.ap.bib_backend.options.GenreGetOptions;
import edu.ap.bib_backend.projection.GenreProjection;

@Service
public class GenreService {
    private final ReviewRepository reviewRepository;
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public GenreService(GenreRepository genreRepository, GenreMapper genreMapper, ReviewRepository reviewRepository) {
        this.genreRepository = genreRepository;
        this.genreMapper = genreMapper;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public GenreDTO createGenre(GenreDTO genreDTO) {
        if (genreRepository.existsById(genreDTO.name())) {
            throw new ResourceAlreadyExistsException("name", genreDTO.name(),
                    "Genre met de naam '" + genreDTO.name() + "' bestaat al.");
        }

        Genre genre = genreMapper.toEntity(genreDTO);
        genreRepository.save(genre);
        return genreMapper.toDto(genre);
    }

    public PageDTO<GenreDTO> getPage(int page, int pageSize, GenreGetOptions options) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("name").ascending());

        Page<? extends GenreProjection> genres;

        if (options.isIncludeBookCount() || options.getBookCount() != null) {
            genres = genreRepository.findAllFiltered(
                    options.getSearch(),
                    options.getBookCount(),
                    pageable);
        } else if (!options.getSearch().isBlank()) {
            genres = genreRepository.searchByNamerOrDescriptionOrColor(options.getSearch(), pageable);
        } else {
            genres = genreRepository.findAll(pageable);
        }

        List<GenreDTO> data = genres.getContent().stream().map(genreMapper::toDto).toList();
        PaginatedMeta meta = PageMapper.toPaginatedMeta(genres);

        return new PageDTO<GenreDTO>(data, meta);
    }

    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(genreMapper::toDto)
                .toList();
    }

    @Transactional
    public GenreDTO updateGenre(String name, GenreDTO inputDTO) {

        Genre genre = genreRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(name,
                        "Deze genre bestaat niet en kan niet gewijzigd worden"));

        genre.setColor(inputDTO.color());
        genre.setDescription(inputDTO.description());

        Genre saved = genreRepository.save(genre);
        GenreDTO outputDTO = genreMapper.toDto(saved);

        return outputDTO;
    }

    public GenreStatsDTO getAllGenreStats() {
        var genres = genreRepository.findAllIncludeBookCount(null).getContent();

        int unActiveCount = genres.stream().filter(g -> g.getBookCount() <= 0).toList().size();
        GenreStatsDTO stats = new GenreStatsDTO(genres.size(), unActiveCount);

        return stats;
    }

    @Transactional
    public void deleteById(String name) {
        Genre genre = genreRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(name,
                        "Deze genre bestaat niet en kan niet verwijderd worden"));

        genreRepository.deleteById(genre.getName());
    }

    @Transactional
    public GenreDTO update(String name, GenreDTO inputDTO) {

        Genre genre = genreRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(name,
                        "Deze genre bestaat niet en kan niet gewijzigd worden"));

        genre.setColor(inputDTO.color());
        genre.setDescription(inputDTO.description());

        Genre saved = genreRepository.save(genre);
        GenreDTO outputDTO = genreMapper.toDto(saved);

        return outputDTO;
    }
}