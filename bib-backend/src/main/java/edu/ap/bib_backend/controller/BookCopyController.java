package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book-copies")
public class BookCopyController {

    private final BookCopyRepository bookCopyRepository;
    private final CurrentUserService currentUserService;

    public BookCopyController(BookCopyRepository bookCopyRepository, CurrentUserService currentUserService) {
        this.bookCopyRepository = bookCopyRepository;
        this.currentUserService = currentUserService;
    }

    @PatchMapping("/{copyId}/condition")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookCopyConditionUpdateDTO> updateCondition(
            @PathVariable Long copyId,
            @Valid @RequestBody BookCopyConditionUpdateDTO request,
            Authentication authentication) {
        Campus campus = currentUserService.getCurrentUserCampus(authentication);
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new IllegalStateException("Boekexemplaar niet gevonden."));

        if (copy.getCampus() == null || !copy.getCampus().getId().equals(campus.getId())) {
            throw new IllegalStateException("Boekexemplaar niet gevonden in deze campus.");
        }

        copy.setCondition(request.condition());
        bookCopyRepository.save(copy);

        return ResponseEntity.ok(new BookCopyConditionUpdateDTO(copy.getId(), copy.getCondition()));
    }
}
