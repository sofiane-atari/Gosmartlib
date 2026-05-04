package edu.ap.bib_backend.controller;

import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.service.TagService;
import jakarta.validation.Valid;

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
            @RequestParam(defaultValue = "false") boolean includeBookCount) {
        return tagService.getTagsByPage(
                page,
                pageSize,
                search,
                bookCount,
                includeBookCount);
    }

    @GetMapping("/stats")
    public TagStatsDTO getTagStats() {
        return tagService.getTagStats();
    }

    @PostMapping
    public TagDTO createTag(@Valid @RequestBody TagDTO tagDTO) {
        return tagService.createTag(tagDTO);
    }

    @PutMapping("/{id}")
    public TagDTO updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO tagDTO) {
        return tagService.updateTag(id, tagDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
    }
}