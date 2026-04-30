package edu.ap.bib_backend.service.lessontips;


import edu.ap.bib_backend.dto.lessontips.BookLessonTipPreviewResponseDto;
import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipPreviewResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.lessontips.LessonTipRepository;
import edu.ap.bib_backend.mapper.lessontips.LessonTipMapper;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LessonTipService {

    private static final int BOOK_DETAIL_PREVIEW_LIMIT = 3;

    private final LessonTipRepository lessonTipRepository;
    private final LessonTipBookLinkService lessonTipBookLinkService;
    private final LessonTipMapper lessonTipMapper;
    private final LessonTipCreatorRoleService lessonTipCreatorRoleService;
    private final LessonTipAuthorizationService lessonTipAuthorizationService;
    private final BookRepository bookRepository;


    public LessonTipService(
            LessonTipRepository lessonTipRepository,
            LessonTipBookLinkService lessonTipBookLinkService,
            LessonTipMapper lessonTipMapper,
            LessonTipCreatorRoleService lessonTipCreatorRoleService,
            LessonTipAuthorizationService lessonTipAuthorizationService,
            BookRepository bookRepository

    ) {
        this.lessonTipRepository = lessonTipRepository;
        this.lessonTipBookLinkService = lessonTipBookLinkService;
        this.lessonTipMapper = lessonTipMapper;
        this.lessonTipCreatorRoleService = lessonTipCreatorRoleService;
        this.lessonTipAuthorizationService = lessonTipAuthorizationService;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public BookLessonTipPreviewResponseDto getLessonTipPreviewByBookId(Long bookId) {
        ensureBookExists(bookId);

        List<LessonTip> lessonTips =
                lessonTipRepository.findTop3ByBookLinksBookIdOrderByUpdatedAtDesc(bookId);

        long totalCount = lessonTipRepository.countByBookLinksBookId(bookId);

        List<LessonTipPreviewResponseDto> previewLessonTips =
                lessonTipMapper.toPreviewResponseList(lessonTips);

        return new BookLessonTipPreviewResponseDto(
                previewLessonTips,
                totalCount,
                totalCount > BOOK_DETAIL_PREVIEW_LIMIT
        );
    }

    @Transactional(readOnly = true)
    public List<LessonTipPreviewResponseDto> getLessonTipPreviewsByBookId(Long bookId) {
        ensureBookExists(bookId);

        List<LessonTip> lessonTips =
                lessonTipRepository.findAllByBookLinksBookIdOrderByUpdatedAtDesc(bookId);

        return lessonTipMapper.toPreviewResponseList(lessonTips);
    }

    @Transactional(readOnly = true)
    public List<LessonTipResponseDto> getAllLessonTips() {
        return lessonTipRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonTipResponseDto getLessonTipById(Long id) {
        LessonTip lessonTip = findLessonTipOrThrow(id);
        return toResponse(lessonTip);
    }

    public LessonTipResponseDto createLessonTip(CreateLessonTipRequestDto request, String currentUserId) {
        LessonTip lessonTip = new LessonTip();
        lessonTip.setCreatedByUserId(currentUserId); 
        applyRequestFields(lessonTip, request);

        LessonTip savedLessonTip = lessonTipRepository.save(lessonTip);
        return toResponse(savedLessonTip);
    }

    public LessonTipResponseDto updateLessonTip(
            Long id,
            CreateLessonTipRequestDto request,
            Authentication authentication
    ) {
        LessonTip lessonTip = findLessonTipOrThrow(id);
        lessonTipAuthorizationService.assertCanModify(lessonTip, authentication);
        applyRequestFields(lessonTip, request);

        LessonTip savedLessonTip = lessonTipRepository.save(lessonTip);
        return toResponse(savedLessonTip);
    }

    public void deleteLessonTip(Long id, Authentication authentication) {
        LessonTip lessonTip = findLessonTipOrThrow(id);
        lessonTipAuthorizationService.assertCanModify(lessonTip, authentication);
        lessonTipRepository.delete(lessonTip);
    }

    private void applyRequestFields(LessonTip lessonTip, CreateLessonTipRequestDto request) {
        lessonTip.setTitle(request.title().trim());
        lessonTip.setDescription(request.description().trim());
        lessonTip.setLessonTipType(request.lessonTipType());
        lessonTip.setSubject(normalizeNullableValue(request.subject()));
        lessonTip.setEducationLevel(normalizeNullableValue(request.educationLevel()));
        lessonTip.setKeywords(normalizeNullableValue(request.keywords()));
        lessonTipBookLinkService.replaceBookLinks(lessonTip, request.linkedBookIds());
    }

    private LessonTipResponseDto toResponse(LessonTip lessonTip) {
        Role createdByRole = lessonTipCreatorRoleService.getCreatorRole(lessonTip.getCreatedByUserId());
        return lessonTipMapper.toResponse(lessonTip, createdByRole);
    }

    private LessonTip findLessonTipOrThrow(Long id) {
        return lessonTipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Lestip niet gevonden met id: " + id
                ));
    }


    private String normalizeNullableValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isBlank() ? null : trimmedValue;
    }

    private void ensureBookExists(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException(
                    "id",
                    "Boek niet gevonden met id: " + bookId
            );
        }
    }

}