package edu.ap.bib_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.mapper.PageMapper;
import edu.ap.bib_backend.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/books")
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

    @PostMapping
    public ResponseEntity<BookDTO> addBook(@RequestBody BookDTO dto) {
        System.out.println("Ontvangen in backend: " + dto);
        BookDTO savedBook = bs.addBook(dto);
        System.out.println("Opgeslagen boek: " + savedBook);
        return ResponseEntity.ok(savedBook);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDTO> getBookByISBN(@PathVariable String isbn) {
        BookDTO book = bs.fetchBookByISBN(isbn);
        return ResponseEntity.ok(book);
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
}
