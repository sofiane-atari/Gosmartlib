package edu.ap.bib_backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
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
                        book.getImage_url(),
                        book.getDescription(),
                        book.getRelease_year(),
                        book.getAvailability(),
                        book.getCopies()

                ))
                .collect(Collectors.toList());
    }

    private BookDTO mapToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setISBN(book.getISBN());
        dto.setPages(book.getPages());
        dto.setImage_url(book.getImage_url());
        dto.setAvailability(book.getAvailability());
        dto.setCopies(book.getCopies());
        dto.setDescription(book.getDescription());
        dto.setRelease_year(book.getRelease_year());
        return dto;
    }

    public BookDTO getBookById(Long id) {
        return br.findById(id)
                .map(book -> mapToDTO(book)) // Zet Book om naar BookDTO
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
    }

}