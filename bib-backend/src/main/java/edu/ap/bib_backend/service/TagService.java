package edu.ap.bib_backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.Tag;
import edu.ap.bib_backend.jpa.repository.TagRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;

    public TagService(TagRepository tagRepository, Optional<CurrentUserService> currentUserService) {
        this.tagRepository = tagRepository;
        this.currentUserService = currentUserService.orElse(null);
    }


    public PageDTO<TagDTO> getTagsByPage(
            int page,
            int pageSize,
            String search,
            String bookCount,
            boolean includeBookCount,
            Authentication authentication) {
        return getTagsByPageForSchool(page, pageSize, search, bookCount, includeBookCount, resolveSchool(authentication).getId());
    }

    public PageDTO<TagDTO> getTagsByPage(
            int page,
            int pageSize,
            String search,
            String bookCount,
            boolean includeBookCount) {
        return getTagsByPageForSchool(page, pageSize, search, bookCount, includeBookCount, null);
    }

    public TagStatsDTO getTagStats(Authentication authentication) {
        Long schoolId = resolveSchool(authentication).getId();
        Long totalItems = tagRepository.countBySchoolIdOrNull(schoolId);
        Long unActiveTags = tagRepository.countUnusedTags(schoolId);

        return new TagStatsDTO(totalItems, unActiveTags);
    }

    public TagStatsDTO getTagStats() {
        Long totalItems = tagRepository.countBySchoolIdOrNull(null);
        Long unActiveTags = tagRepository.countUnusedTags(null);

        return new TagStatsDTO(totalItems, unActiveTags);
    }

    public TagDTO createTag(TagDTO tagDTO, Authentication authentication) {
        return createTagForSchool(tagDTO, resolveSchool(authentication));
    }

    public TagDTO createTag(TagDTO tagDTO) {
        return createTagForSchool(tagDTO, null);
    }

    private TagDTO createTagForSchool(TagDTO tagDTO, School school) {
        Long schoolId = school != null ? school.getId() : null;
        String name = normalizeName(tagDTO.name());
        String description = normalizeDescription(tagDTO.description());

        if (name == null) {
            throw new IllegalArgumentException("Naam is verplicht.");
        }

        if (tagRepository.existsByNameInSchool(name, schoolId)) {
            throw new IllegalArgumentException("Deze tag bestaat al.");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setSchool(school);

        Tag savedTag = tagRepository.save(tag);

        return toDTO(savedTag, true);
    }

    public TagDTO updateTag(Long id, TagDTO tagDTO, Authentication authentication) {
        return updateTagForSchool(id, tagDTO, resolveSchool(authentication));
    }

    public TagDTO updateTag(Long id, TagDTO tagDTO) {
        return updateTagForSchool(id, tagDTO, null);
    }

    private TagDTO updateTagForSchool(Long id, TagDTO tagDTO, School school) {
        Long schoolId = school != null ? school.getId() : null;
        Tag tag = tagRepository.findByIdInSchool(id, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Tag niet gevonden."));

        String name = normalizeName(tagDTO.name());
        String description = normalizeDescription(tagDTO.description());

        if (name == null) {
            throw new IllegalArgumentException("Naam is verplicht.");
        }

        if (tagRepository.existsByNameInSchoolAndIdNot(name, schoolId, id)) {
            throw new IllegalArgumentException("Deze tag bestaat al.");
        }

        tag.setName(name);
        tag.setDescription(description);

        Tag savedTag = tagRepository.save(tag);

        return toDTO(savedTag, true);
    }

    public void deleteTag(Long id, Authentication authentication) {
        deleteTagForSchool(id, resolveSchool(authentication));
    }

    public void deleteTag(Long id) {
        deleteTagForSchool(id, null);
    }

    private void deleteTagForSchool(Long id, School school) {
        Long schoolId = school != null ? school.getId() : null;
        Tag tag = tagRepository.findByIdInSchool(id, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Tag niet gevonden."));

        if (getBookCount(tag) > 0) {
            throw new IllegalArgumentException("Deze tag is gekoppeld aan boeken en kan niet verwijderd worden.");
        }

        tagRepository.delete(tag);
    }

    private PageDTO<TagDTO> getTagsByPageForSchool(
            int page,
            int pageSize,
            String search,
            String bookCount,
            boolean includeBookCount,
            Long schoolId) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        Page<Tag> tagPage = tagRepository.findBySearch(
                schoolId,
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

    private School resolveSchool(Authentication authentication) {
        if (currentUserService == null) {
            throw new IllegalStateException("CurrentUserService is niet beschikbaar.");
        }

        return currentUserService.getCurrentUserSchool(authentication);
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
