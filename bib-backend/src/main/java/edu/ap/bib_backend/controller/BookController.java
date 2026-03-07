package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
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

}
