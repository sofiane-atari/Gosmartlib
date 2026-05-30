package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.service.BookCopyService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book-copies")
public class BookCopyController {

    private final BookCopyRepository bookCopyRepository;
    private final CurrentUserService currentUserService;
    private final BookCopyService bookCopyService;

    public BookCopyController(BookCopyRepository bookCopyRepository, CurrentUserService currentUserService,
            BookCopyService bookCopyService) {
        this.bookCopyRepository = bookCopyRepository;
        this.currentUserService = currentUserService;
        this.bookCopyService = bookCopyService;
    }

    @PatchMapping("/{copyId}/condition")
    @PreAuthorize("hasRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<BookCopyConditionUpdateDTO> updateCondition(
            @PathVariable Long copyId,
            @Valid @RequestBody BookCopyConditionUpdateDTO request,
            Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new IllegalStateException("Boekexemplaar niet gevonden."));

        if (copy.getCampus() == null || copy.getCampus().getSchool() == null
                || !copy.getCampus().getSchool().getId().equals(school.getId())) {
            throw new IllegalStateException("Boekexemplaar niet gevonden in deze school.");
        }

        copy.setCondition(request.condition());
        bookCopyRepository.save(copy);

        return ResponseEntity.ok(new BookCopyConditionUpdateDTO(copy.getId(), copy.getCondition()));
    }

    @GetMapping("/book/{bookId}/by-school")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<List<BookCopy>> getCopiesByBookIdForSchool(
            @PathVariable Long bookId,
            Authentication authentication) {
        return ResponseEntity.ok(bookCopyService.getCopiesByBookIdForSchool(bookId, authentication));
    }

    @DeleteMapping("/{copyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCopy(@PathVariable Long copyId, Authentication authentication) {
        bookCopyService.deleteCopy(copyId, authentication);
    }

    @DeleteMapping("/book/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllCopiesByBookIdForSchool(@PathVariable Long bookId, Authentication authentication) {
        bookCopyService.deleteAllCopiesByBookIdForSchool(bookId, authentication);
    }
}
