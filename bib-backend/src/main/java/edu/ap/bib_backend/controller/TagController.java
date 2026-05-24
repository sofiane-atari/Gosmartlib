package edu.ap.bib_backend.controller;

import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.service.TagService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public PageDTO<TagDTO> getTagsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String bookCount,
            @RequestParam(defaultValue = "false") boolean includeBookCount,
            Authentication authentication) {
        if (authentication == null) {
            return tagService.getTagsByPage(
                    page,
                    pageSize,
                    search,
                    bookCount,
                    includeBookCount);
        }

        return tagService.getTagsByPage(
                page,
                pageSize,
                search,
                bookCount,
                includeBookCount,
                authentication);
    }

    @GetMapping("/stats")
    public TagStatsDTO getTagStats(Authentication authentication) {
        if (authentication == null) {
            return tagService.getTagStats();
        }

        return tagService.getTagStats(authentication);
    }

    @PostMapping
    public TagDTO createTag(@Valid @RequestBody TagDTO tagDTO, Authentication authentication) {
        if (authentication == null) {
            return tagService.createTag(tagDTO);
        }

        return tagService.createTag(tagDTO, authentication);
    }

    @PutMapping("/{id}")
    public TagDTO updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO tagDTO,
            Authentication authentication) {
        if (authentication == null) {
            return tagService.updateTag(id, tagDTO);
        }

        return tagService.updateTag(id, tagDTO, authentication);
    }

    @DeleteMapping("/{id}")
    public void deleteTag(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            tagService.deleteTag(id);
            return;
        }

        tagService.deleteTag(id, authentication);
    }
}
