package edu.ap.bib_backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository br;

    public BookService(BookRepository repo) {
        br = repo;
    }

    public List<BookDTO> getAllBooks() {
        return br.findAll()
                .stream()
                .map(book -> new BookDTO(
                        book.getId(),
                        book.getISBN(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getPages(),
                        book.getImage_url()))
                .collect(Collectors.toList());
    }

}