package edu.ap.bib_backend.controller.lessontips;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.service.lessontips.LessonTipService;
import edu.ap.bib_backend.service.user.CurrentUserService;


@RestController
@RequestMapping("/lesson-tips")
public class LessonTipController {

    private final LessonTipService lessonTipService;
    private final CurrentUserService currentUserService;

    public LessonTipController(LessonTipService lessonTipService, CurrentUserService currentUserService) {
        this.lessonTipService = lessonTipService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<PageDTO<LessonTipResponseDto>> getAllLessonTips(
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<LessonTipResponseDto> lessonTipPage =
                lessonTipService.getAllLessonTips(bookId, page, size);

        return ResponseEntity.ok(
                new PageDTO<>(
                        lessonTipPage.getContent(),
                        PageMapper.toPaginatedMeta(lessonTipPage)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonTipResponseDto> getLessonTip(@PathVariable Long id) {
        return ResponseEntity.ok(lessonTipService.getLessonTipById(id));
    }

    @PostMapping
    public ResponseEntity<LessonTipResponseDto> createLessonTip(
            @Valid @RequestBody CreateLessonTipRequestDto request,
            Authentication authentication) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);
        LessonTipResponseDto createdLessonTip = lessonTipService.createLessonTip(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdLessonTip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LessonTipResponseDto> updateLessonTip(
            @PathVariable Long id,
            @Valid @RequestBody CreateLessonTipRequestDto request,
            Authentication authentication) {
        LessonTipResponseDto updatedLessonTip = lessonTipService.updateLessonTip(id, request, authentication);

        return ResponseEntity.ok(updatedLessonTip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLessonTip(
            @PathVariable Long id,
            Authentication authentication) {
        lessonTipService.deleteLessonTip(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
