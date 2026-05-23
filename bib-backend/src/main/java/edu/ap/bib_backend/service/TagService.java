package edu.ap.bib_backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.jpa.entity.Tag;
import edu.ap.bib_backend.jpa.repository.TagRepository;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public PageDTO<TagDTO> getTagsByPage(
            int page,
            int pageSize,
            String search,
            String bookCount,
            boolean includeBookCount) {

        PageRequest pageRequest = PageRequest.of(page, pageSize);

        Page<Tag> tagPage = tagRepository.findBySearch(
                normalizeSearch(search),
                pageRequest);

        List<TagDTO> data = tagPage
                .getContent()
                .stream()
                .map(tag -> toDTO(tag, includeBookCount))
                .toList();

        if (bookCount != null && !bookCount.isBlank()) {
            long parsedBookCount = Long.parseLong(bookCount);

            data = data.stream()
                    .filter(tag -> tag.bookCount() != null && tag.bookCount() == parsedBookCount)
                    .toList();
        }

        PaginatedMeta meta = new PaginatedMeta(
                tagPage.getNumber(),
                tagPage.getSize(),
                tagPage.getTotalElements(),
                tagPage.getTotalPages());

        return new PageDTO<>(data, meta);
    }

    public TagStatsDTO getTagStats() {
        Long totalItems = tagRepository.count();
        Long unActiveTags = tagRepository.countUnusedTags();

        return new TagStatsDTO(totalItems, unActiveTags);
    }

    public TagDTO createTag(TagDTO tagDTO) {
        String name = normalizeName(tagDTO.name());
        String description = normalizeDescription(tagDTO.description());

        if (name == null) {
            throw new IllegalArgumentException("Naam is verplicht.");
        }

        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Deze tag bestaat al.");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);

        Tag savedTag = tagRepository.save(tag);

        return toDTO(savedTag, true);
    }

    public TagDTO updateTag(Long id, TagDTO tagDTO) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag niet gevonden."));

        String name = normalizeName(tagDTO.name());
        String description = normalizeDescription(tagDTO.description());

        if (name == null) {
            throw new IllegalArgumentException("Naam is verplicht.");
        }

        if (tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Deze tag bestaat al.");
        }

        tag.setName(name);
        tag.setDescription(description);

        Tag savedTag = tagRepository.save(tag);

        return toDTO(savedTag, true);
    }

    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag niet gevonden."));

        if (getBookCount(tag) > 0) {
            throw new IllegalArgumentException("Deze tag is gekoppeld aan boeken en kan niet verwijderd worden.");
        }

        tagRepository.delete(tag);
    }

    private TagDTO toDTO(Tag tag, boolean includeBookCount) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getDescription(),
                includeBookCount ? getBookCount(tag) : null,
                tag.getCreatedAt());
    }

    private Long getBookCount(Tag tag) {
        return tagRepository.countBookAndCopyLinks(tag.getId());
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isBlank()) {
            return null;
        }

        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }
}
