package edu.ap.bib_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable("id") Long id) {
        BookDTO book = bs.getBookById(id);
        return ResponseEntity.ok(book);
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

    @GetMapping("/years/range")
    public ResponseEntity<Map<String, Integer>> getYearRange() {
        Map<String, Integer> range = new HashMap<>();
        range.put("min", bs.getMinReleaseYear());
        range.put("max", bs.getMaxReleaseYear());
        return ResponseEntity.ok(range);
    }

    @GetMapping("/pages/max")
    public ResponseEntity<Integer> getMaxPages() {
        return ResponseEntity.ok(bs.getMaxPages());
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
