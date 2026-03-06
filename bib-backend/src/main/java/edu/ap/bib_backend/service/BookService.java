package edu.ap.bib_backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
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
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BookDTO createBookWithoutISBN(BookDTO dto) {
        Book book = mapToEntity(dto);
        Book savedBook = br.save(book);
        return mapToDTO(savedBook);
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

    private Book mapToEntity(BookDTO dto) {
        Book book = new Book();

        book.setId(dto.getId());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setISBN(dto.getISBN());
        book.setPages(dto.getPages());
        book.setImage_url(dto.getImage_url());
        book.setAvailability(dto.getAvailability());
        book.setCopies(dto.getCopies());
        book.setDescription(dto.getDescription());
        book.setRelease_year(dto.getRelease_year());

        return book;
    }

    public BookDTO getBookById(Long id) {
        return br.findById(id)
                .map(book -> mapToDTO(book)) // Zet Book om naar BookDTO
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
    }

    public BookDTO fetchBookByISBN(String isbn) {

        RestTemplate restTemplate = new RestTemplate();

        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        Map<String, Object> bookData = (Map<String, Object>) response.get("ISBN:" + isbn);

        BookDTO dto = new BookDTO();

        dto.setISBN(isbn);
        dto.setTitle((String) bookData.get("title"));

        List<Map<String, String>> authors = (List<Map<String, String>>) bookData.get("authors");
        dto.setAuthor(authors.get(0).get("name"));

        dto.setPages((Integer) bookData.get("number_of_pages"));

        Map<String, String> cover = (Map<String, String>) bookData.get("cover");
        if (cover != null) {
            dto.setImage_url(cover.get("medium"));
        }

        dto.setAvailability(Availability.AVAILABLE); //nog aanpassen
        dto.setCopies(1);//nog aanpassen

        return dto;
    }

}