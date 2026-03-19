package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.specification.BookSpecification;

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
        dto.setReviewScore(book.getReviewScore() != null ? book.getReviewScore() : 0.0);
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

    public Integer getMaxPages() {
        Integer max = br.findMaxPages();
        return max != null ? max : 1500;
    }

    public Integer getMinPages() {
        Integer min = br.findMinPages();
        return min != null ? min : 0;
    }

    public List<BookDTO> getMostReadBooks(int limit) {
        List<Book> books = br.findMostReadBook(limit);

        if (books.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen uitgeleende boeken gevonden");
        }

        return mapToDTOList(books);
    }

    public List<BookDTO> getNewestBooks(int limit) {
        List<Book> books = br.findNewestBooks(PageRequest.of(0, limit));
        return mapToDTOList(books);
    }

    private List<BookDTO> mapToDTOList(List<Book> books) {
        List<BookDTO> dtos = new ArrayList<>();

        for (Book book : books)
            dtos.add(mapToDTO(book));

        return dtos;
    }

    public Page<BookDTO> getFilteredBooks(
        String searchTerm,
        BookAvailabilityFilter availabilityFilter,
        Integer minPages,
        Integer maxPages,
        int page,
        int size,
        BookSortOption sortBy
    ) {
        String normalizedSearchTerm = normalizeSearchTerm(searchTerm);
        int validatedPage = validatePage(page);
        int validatedSize = validateSize(size);
        validatePageRange(minPages, maxPages);

        Availability availability = mapAvailabilityFilter(availabilityFilter);
        Sort sort = mapSort(sortBy);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sort);

        Specification<Book> specification = BookSpecification.withFilters(
                normalizedSearchTerm,
                availability,
                minPages,
                maxPages
        );

        Page<Book> bookPage = br.findAll(specification, pageable);
        return bookPage.map(this::mapToDTO);
    }

    private String normalizeSearchTerm(String searchTerm) {
    if (searchTerm == null || searchTerm.isBlank()) {
        return null;
    }

    return searchTerm.trim();
    }

    private int validatePage(int page) {
        return Math.max(page, 0);
    }

    private int validateSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private void validatePageRange(Integer minPages, Integer maxPages) {
        if (minPages != null && maxPages != null && minPages > maxPages) {
            throw new IllegalArgumentException("minPages mag niet groter zijn dan maxPages.");
        }
    }

    private Availability mapAvailabilityFilter(BookAvailabilityFilter availabilityFilter) {
        if (availabilityFilter == null || availabilityFilter == BookAvailabilityFilter.ALL) {
            return null;
        }

        if (availabilityFilter == BookAvailabilityFilter.AVAILABLE) {
            return Availability.AVAILABLE;
        }

        return null;
    }

    private Sort mapSort(BookSortOption sortBy) {
        if (sortBy == null) {
            return Sort.by("title").ascending();
        }

        return switch (sortBy) {
            case AZ -> Sort.by("title").ascending();
            case ZA -> Sort.by("title").descending();
            case NEWEST -> Sort.by("releaseYear").descending();
            case RATING -> Sort.by("reviewScore").descending();
        };
    }
}