package edu.ap.bib_backend.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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

    public BookDTO addBook(BookDTO dto) {
        System.out.println("Ontvangen DTO: " + dto);
        Book book = mapToEntity(dto);
        System.out.println("Mapped entity: " + book);
        Book saved = br.save(book);
        System.out.println("Saved entity: " + saved);
        return mapToDTO(saved);
    }

    private BookDTO mapToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setISBN(book.getISBN());
        dto.setPages(book.getPages());
        dto.setImageUrl(book.getImageUrl());
        dto.setAvailability(book.getAvailability());
        dto.setCopies(book.getCopies());
        dto.setDescription(book.getDescription());
        dto.setReleaseYear(book.getReleaseYear());
        return dto;
    }

    private Book mapToEntity(BookDTO dto) {
        Book book = new Book();

        book.setId(dto.getId());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setISBN(dto.getISBN());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setAvailability(dto.getAvailability());
        book.setCopies(dto.getCopies());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());

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
        String rawDate = (String) bookData.get("publish_date");
        int year = 0;
        List<Map<String, String>> authors = (List<Map<String, String>>) bookData.get("authors");

        if (rawDate != null) {
            // Zoek naar 4 opeenvolgende cijfers (\d{4})
            java.util.regex.Matcher matcher = Pattern.compile("\\d{4}").matcher(rawDate);
            if (matcher.find()) {
                year = Integer.parseInt(matcher.group());
            }
        }

        Map<String, String> cover = (Map<String, String>) bookData.get("cover");
        if (cover != null) {
            dto.setImageUrl(cover.get("medium"));
        }

        dto.setISBN(isbn);
        dto.setTitle((String) bookData.get("title"));
        dto.setAuthor(authors.get(0).get("name"));
        dto.setPages((Integer) bookData.get("number_of_pages"));
        dto.setReleaseYear(year);
        dto.setCopies(1);// nog aanpassen

        return dto;
    }

}