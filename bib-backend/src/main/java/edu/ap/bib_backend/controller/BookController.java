package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = { "https://frontend.localhost/", "https://gosmartlib.be/", "http://localhost:4200/" })
public class BookController {
    private final BookService bs;

    public BookController(BookService bs) {
        this.bs = bs;
    }

    @GetMapping
    public List<BookDTO> getAllBooks() {
        return bs.getAllBooks();
    }

}
