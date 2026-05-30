package edu.ap.bib_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.BookCopyLocationDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;

@Slf4j
@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final BookLoanRepository bookLoanRepository;
    private final CurrentUserService currentUserService;

    public BookCopyService(
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            ReviewRepository reviewRepository,
            BookLoanRepository bookLoanRepository,
            CurrentUserService currentUserService) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.currentUserService = currentUserService;
    }

    public List<BookCopy> getCopiesByBookIdForSchool(Long bookId, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        return bookCopyRepository.findAllByBookIdAndCampusSchoolId(bookId, school.getId());
    }

    @Transactional
    public void deleteCopy(Long copyId, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Boekexemplaar niet gevonden."));

        if (copy.getCampus() == null || copy.getCampus().getSchool() == null
                || !copy.getCampus().getSchool().getId().equals(school.getId())) {
            throw new IllegalArgumentException("Boekexemplaar niet gevonden in deze school.");
        }

        Book book = copy.getBook();

        bookLoanRepository.deleteByCopyId(copyId);

        bookCopyRepository.delete(copy);

        bookCopyRepository.flush();
        cleanupBookIfNoCopies(book);

        log.info("Boekexemplaar {} van boek '{}' verwijderd door {}.",
                copyId, book.getTitle(), authentication.getName());
    }

    @Transactional
    public void deleteAllCopiesByBookIdForSchool(Long bookId, Authentication authentication) {
        School school = currentUserService.getCurrentUserSchool(authentication);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden."));

        List<BookCopy> copies = bookCopyRepository.findAllByBookIdAndCampusSchoolId(bookId, school.getId());
        if (copies.isEmpty()) {
            throw new IllegalArgumentException("Geen exemplaren gevonden in deze school.");
        }

        List<Long> copyIds = copies.stream().map(BookCopy::getId).toList();
        bookLoanRepository.deleteByCopyIdIn(copyIds);

        bookCopyRepository.deleteAll(copies);
        bookCopyRepository.flush();
        cleanupBookIfNoCopies(book);

        log.info("{} exemplaren van boek '{}' verwijderd door {} (school {}).",
                copies.size(), book.getTitle(), authentication.getName(), school.getId());
    }

    private void cleanupBookIfNoCopies(Book book) {
        long remainingCopies = bookCopyRepository.countByBookId(book.getId());
        if (remainingCopies > 0) {
            return;
        }

        log.info("Boek '{}' heeft geen exemplaren meer en wordt automatisch verwijderd.", book.getTitle());

        reviewRepository.deleteByBookId(book.getId());
        bookLoanRepository.deleteByCopyBookId(book.getId());

        book.getGenres().clear();
        book.getTags().clear();
        bookRepository.delete(book);
    }
}
