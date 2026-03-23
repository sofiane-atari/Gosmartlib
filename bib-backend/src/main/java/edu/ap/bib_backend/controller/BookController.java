package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bs;

    public BookController(BookService bs) {
        this.bs = bs;
    }

    @GetMapping
    public List<BookDTO> getAllBooks() {
        return bs.getAllBooks();
    }

    @GetMapping("/catalog")
    public PageDTO<BookDTO> getFilteredBooks(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BookAvailabilityFilter availability,
            @RequestParam(required = false) Integer minPages,
            @RequestParam(required = false) Integer maxPages,
            @RequestParam(required = false) BookSortOption sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<BookDTO> bookPage = bs.getFilteredBooks(
                searchTerm,
                availability,
                minPages,
                maxPages,
                page,
                size,
                sortBy
        );

        return new PageDTO<>(
                bookPage.getContent(),
                PageMapper.toPaginatedMeta(bookPage)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable("id") Long id) {
        BookDTO book = bs.getBookById(id);
        return ResponseEntity.ok(book);
    }


    @GetMapping("/{id}/related-carousel")
    public ResponseEntity<List<BookDTO>> getRelatedBooksCarousel(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "10", required = false) int limit) {
        return ResponseEntity.ok(bs.getRelatedBooksCarousel(id, limit));
    }


    @PostMapping
    public ResponseEntity<BookDTO> addBook(@RequestBody BookDTO dto) {
        BookDTO savedBook = bs.addBook(dto);
        return ResponseEntity.ok(savedBook);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDTO> getBookByISBN(@PathVariable String isbn) {
        BookDTO book = bs.fetchBookByISBN(isbn);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/isbn/{isbn}/preview")
    public ResponseEntity<BookPreviewDTO> getBookPreviewByISBN(@PathVariable String isbn) {
        return ResponseEntity.ok(bs.fetchPreviewByISBN(isbn));
    }


    @GetMapping("/pages/max")
    public ResponseEntity<Integer> getMaxPages() {
        return ResponseEntity.ok(bs.getMaxPages());
    }

    @GetMapping("/pages/min")
    public ResponseEntity<Integer> getMinPages() {
        return ResponseEntity.ok(bs.getMinPages());
    }

    @GetMapping("/most-read")
    public ResponseEntity<List<BookDTO>> getMostReadBooks(
            @RequestParam(defaultValue = "3", required = false) int limit) {
        return ResponseEntity.ok(bs.getMostReadBooks(limit));
    }

    @GetMapping("/newest")
    public ResponseEntity<List<BookDTO>> getNewestBooks(
            @RequestParam(defaultValue = "3", required = false) int limit) {
        return ResponseEntity.ok(bs.getNewestBooks(limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookCandidateDTO>> searchBookCandidates(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam Integer releaseYear) {
        List<BookCandidateDTO> candidates = bs.searchBookCandidates(title, author, releaseYear);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/select")
    public ResponseEntity<BookDTO> selectBook(@RequestParam String isbn13) {
        BookDTO book = bs.fetchBookByISBN(isbn13);
        return ResponseEntity.ok(book);
    }

    //@PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO dto) {
        BookDTO updatedBook = bs.updateBook(id, dto);
        return ResponseEntity.ok(updatedBook);
    }
}
