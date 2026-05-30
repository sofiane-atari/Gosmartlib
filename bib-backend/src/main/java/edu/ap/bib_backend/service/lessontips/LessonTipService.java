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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    /**
     * Returns a limited lesson tip preview for a book, including the total count.
     *
     * @param bookId the book id
     * @return preview lesson tips and count information
     * @throws ResourceNotFoundException if the book does not exist
     */
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

    /**
     * Returns all lesson tip previews linked to a book.
     *
     * @param bookId the book id
     * @return lesson tip previews ordered by update date descending
     * @throws ResourceNotFoundException if the book does not exist
     */
    @Transactional(readOnly = true)
    public List<LessonTipPreviewResponseDto> getLessonTipPreviewsByBookId(Long bookId) {
        ensureBookExists(bookId);

        List<LessonTip> lessonTips =
                lessonTipRepository.findAllByBookLinksBookIdOrderByUpdatedAtDesc(bookId);

        return lessonTipMapper.toPreviewResponseList(lessonTips);
    }

    /**
     * Returns paged lesson tips, optionally filtered by book.
     *
     * @param bookId optional book id filter
     * @param page the page index
     * @param size the page size
     * @return paged lesson tip responses
     * @throws ResourceNotFoundException if the book filter does not exist
     */
    @Transactional(readOnly = true)
    public Page<LessonTipResponseDto> getAllLessonTips(
            Long bookId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return resolveLessonTips(bookId, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a lesson tip by id.
     *
     * @param id the lesson tip id
     * @return the lesson tip response
     * @throws ResourceNotFoundException if the lesson tip does not exist
     */
    @Transactional(readOnly = true)
    public LessonTipResponseDto getLessonTipById(Long id) {
        LessonTip lessonTip = findLessonTipOrThrow(id);
        return toResponse(lessonTip);
    }

    /**
     * Creates a lesson tip for the current user.
     *
     * @param request the lesson tip data
     * @param currentUserId the creating user id
     * @return the created lesson tip response
     */
    public LessonTipResponseDto createLessonTip(CreateLessonTipRequestDto request, String currentUserId) {
        LessonTip lessonTip = new LessonTip();
        lessonTip.setCreatedByUserId(currentUserId); 
        applyRequestFields(lessonTip, request);

        LessonTip savedLessonTip = lessonTipRepository.save(lessonTip);
        return toResponse(savedLessonTip);
    }

    /**
     * Updates a lesson tip after checking modification permissions.
     *
     * @param id the lesson tip id
     * @param request the updated lesson tip data
     * @param authentication the current authentication
     * @return the updated lesson tip response
     * @throws ResourceNotFoundException if the lesson tip does not exist
     */
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

    /**
     * Deletes a lesson tip after checking modification permissions.
     *
     * @param id the lesson tip id
     * @param authentication the current authentication
     * @throws ResourceNotFoundException if the lesson tip does not exist
     */
    public void deleteLessonTip(Long id, Authentication authentication) {
        LessonTip lessonTip = findLessonTipOrThrow(id);
        lessonTipAuthorizationService.assertCanModify(lessonTip, authentication);
        lessonTipRepository.delete(lessonTip);
    }

    /**
     * Applies request values and linked books to a lesson tip entity.
     *
     * @param lessonTip the lesson tip to update
     * @param request the source request data
     */
    private void applyRequestFields(LessonTip lessonTip, CreateLessonTipRequestDto request) {
        lessonTip.setTitle(request.title().trim());
        lessonTip.setDescription(request.description().trim());
        lessonTip.setLessonTipType(request.lessonTipType());
        lessonTip.setSubject(normalizeNullableValue(request.subject()));
        lessonTip.setEducationLevel(normalizeNullableValue(request.educationLevel()));
        lessonTip.setKeywords(normalizeNullableValue(request.keywords()));
        lessonTipBookLinkService.replaceBookLinks(lessonTip, request.linkedBookIds());
    }

    /**
     * Maps a lesson tip entity to its response DTO with creator role information.
     *
     * @param lessonTip the lesson tip to map
     * @return the mapped response DTO
     */
    private LessonTipResponseDto toResponse(LessonTip lessonTip) {
        Role createdByRole = lessonTipCreatorRoleService.getCreatorRole(lessonTip.getCreatedByUserId());
        return lessonTipMapper.toResponse(lessonTip, createdByRole);
    }

    /**
     * Finds a lesson tip or throws when it is missing.
     *
     * @param id the lesson tip id
     * @return the found lesson tip
     * @throws ResourceNotFoundException if the lesson tip does not exist
     */
    private LessonTip findLessonTipOrThrow(Long id) {
        return lessonTipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "id",
                        "Lestip niet gevonden met id: " + id
                ));
    }


    /**
     * Trims a nullable value and converts blank values to null.
     *
     * @param value the value to normalize
     * @return the trimmed value, or null when absent or blank
     */
    private String normalizeNullableValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isBlank() ? null : trimmedValue;
    }

    /**
     * Verifies that a book exists.
     *
     * @param bookId the book id
     * @throws ResourceNotFoundException if the book does not exist
     */
    private void ensureBookExists(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException(
                    "id",
                    "Boek niet gevonden met id: " + bookId
            );
        }
    }

    /**
     * Resolves paged lesson tips with an optional book filter.
     *
     * @param bookId optional book id filter
     * @param pageable the paging information
     * @return paged lesson tip entities
     * @throws ResourceNotFoundException if the book filter does not exist
     */
    private Page<LessonTip> resolveLessonTips(Long bookId, Pageable pageable) {
        if (bookId == null) {
            return lessonTipRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }

        ensureBookExists(bookId);
        return lessonTipRepository.findAllByBookLinksBookIdOrderByUpdatedAtDesc(bookId, pageable);
}

}
