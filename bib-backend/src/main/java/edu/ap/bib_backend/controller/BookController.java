package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.lessontips.BookLessonTipPreviewResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipPreviewResponseDto;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.service.book.BookSearchService;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.book.GoogleBooksClient;
import edu.ap.bib_backend.service.book.OpenLibraryClient;
import edu.ap.bib_backend.service.lessontips.LessonTipService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;
    private final BookSearchService bookSearchService;
    private final GoogleBooksClient googleBooksClient;
    private final OpenLibraryClient openLibraryClient;
    private final LessonTipService lessonTipService;

    public BookController(BookService bookService, BookSearchService bookSearchService,
            GoogleBooksClient googleBooksClient, OpenLibraryClient openLibraryClient, LessonTipService lessonTipService) {
        this.bookService = bookService;
        this.bookSearchService = bookSearchService;
        this.googleBooksClient = googleBooksClient;
        this.openLibraryClient = openLibraryClient;
        this.lessonTipService = lessonTipService;
    }

    @GetMapping
    public List<BookDTO> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/catalog")
    public PageDTO<BookDTO> getFilteredBooks(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BookAvailabilityFilter availability,
            @RequestParam(required = false) Integer minPages,
            @RequestParam(required = false) Integer maxPages,
            @RequestParam(required = false) Long readingLevelId,
            @RequestParam(required = false) String genreName,
            @RequestParam(required = false) BookSortOption sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        // AANGEPAST: We gebruiken nu bookSearchService
        Page<BookDTO> bookPage = bookSearchService.getFilteredBooks(
                searchTerm,
                availability,
                minPages,
                maxPages,
                readingLevelId,
                genreName,
                page,
                size,
                sortBy,
                authentication);

        return new PageDTO<>(
                bookPage.getContent(),
                PageMapper.toPaginatedMeta(bookPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(
            @PathVariable("id") Long id ) {
                BookDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/{id}/related-carousel")
    public ResponseEntity<List<BookDTO>> getRelatedBooksCarousel(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "10", required = false) int limit,
            Authentication authentication) {
        return ResponseEntity.ok(bookService.getRelatedBooksCarousel(id, limit, authentication));
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(@RequestBody BookDTO dto) {
        BookDTO savedBook = bookService.addBook(dto);
        return ResponseEntity.ok(savedBook);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDTO> getBookByISBN(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.getBookByISBN(isbn));
    }

    @GetMapping("/isbn/{isbn}/preview")
    public ResponseEntity<BookPreviewDTO> getBookPreviewByISBN(@PathVariable String isbn) {
        return ResponseEntity.ok(openLibraryClient.fetchPreviewByISBN(isbn));
    }

    @GetMapping("/pages/max")
    public ResponseEntity<Integer> getMaxPages() {
        return ResponseEntity.ok(bookService.getMaxPages());
    }

    @GetMapping("/pages/min")
    public ResponseEntity<Integer> getMinPages() {
        return ResponseEntity.ok(bookService.getMinPages());
    }

    @GetMapping("/most-read")
    public ResponseEntity<List<BookDTO>> getMostReadBooks(
            @RequestParam(defaultValue = "3", required = false) int limit,
            Authentication authentication) {
        return ResponseEntity.ok(bookService.getMostReadBooks(limit, authentication));
    }

    @GetMapping("/newest")
    public ResponseEntity<List<BookDTO>> getNewestBooks(
            @RequestParam(defaultValue = "3", required = false) int limit,
            Authentication authentication) {
        return ResponseEntity.ok(bookService.getNewestBooks(limit, authentication));
    }

    @GetMapping("/search-candidates")
    public ResponseEntity<List<BookCandidateDTO>> searchBookCandidates(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam Integer releaseYear) {
        List<BookCandidateDTO> candidates = googleBooksClient.searchBookCandidates(title, author, releaseYear);
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookCandidateDTO>> searchBooksInDb(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer releaseYear) {
        return ResponseEntity.ok(bookService.searchBooksInDb(title, author, releaseYear));
    }

    @PostMapping("/select")
    public ResponseEntity<BookDTO> selectBook(@RequestParam String isbn13) {
        BookDTO book = bookService.getBookByISBN(isbn13);
        return ResponseEntity.ok(book);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO dto) {
        BookDTO updatedBook = bookService.updateBook(id, dto);
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/copies")
    public ResponseEntity<BookDTO> addCopy(@PathVariable Long bookId) {
        BookDTO dto = bookService.addCopyToBook(bookId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{bookId}/lesson-tips")
    public ResponseEntity<List<LessonTipPreviewResponseDto>> getLessonTipsByBookId(
            @PathVariable Long bookId
    ) {
        return ResponseEntity.ok(lessonTipService.getLessonTipPreviewsByBookId(bookId));
    }

    @GetMapping("/{bookId}/lesson-tips/preview")
    public ResponseEntity<BookLessonTipPreviewResponseDto> getLessonTipPreviewByBookId(
            @PathVariable Long bookId
    ) {
        return ResponseEntity.ok(lessonTipService.getLessonTipPreviewByBookId(bookId));
    }
}
