package edu.ap.bib_backend.controller;

import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.service.TagService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
        return withOrWithoutAuth(authentication,
                () -> tagService.getTagsByPage(page, pageSize, search, bookCount, includeBookCount),
                auth -> tagService.getTagsByPage(page, pageSize, search, bookCount, includeBookCount, auth));
    }

    @GetMapping("/stats")
    public TagStatsDTO getTagStats(Authentication authentication) {
        return withOrWithoutAuth(authentication,
                () -> tagService.getTagStats(),
                auth -> tagService.getTagStats(auth));
    }

    @PostMapping
    public TagDTO createTag(@Valid @RequestBody TagDTO tagDTO, Authentication authentication) {
        return withOrWithoutAuth(authentication,
                () -> tagService.createTag(tagDTO),
                auth -> tagService.createTag(tagDTO, auth));
    }

    @PutMapping("/{id}")
    public TagDTO updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO tagDTO,
            Authentication authentication) {
        return withOrWithoutAuth(authentication,
                () -> tagService.updateTag(id, tagDTO),
                auth -> tagService.updateTag(id, tagDTO, auth));
    }

    @DeleteMapping("/{id}")
    public void deleteTag(@PathVariable Long id, Authentication authentication) {
        runWithOrWithoutAuth(authentication,
                () -> tagService.deleteTag(id),
                auth -> tagService.deleteTag(id, auth));
    }

    private <T> T withOrWithoutAuth(Authentication auth, Supplier<T> withoutAuth, Function<Authentication, T> withAuth) {
        if (auth == null) {
            return withoutAuth.get();
        }
        return withAuth.apply(auth);
    }

    private void runWithOrWithoutAuth(Authentication auth, Runnable withoutAuth, Consumer<Authentication> withAuth) {
        if (auth == null) {
            withoutAuth.run();
        } else {
            withAuth.accept(auth);
        }
    }
}
