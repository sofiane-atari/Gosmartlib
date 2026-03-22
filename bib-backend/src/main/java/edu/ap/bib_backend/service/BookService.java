package edu.ap.bib_backend.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.net.URLEncoder;

@Service
public class BookService {

    private String googleBooksApiKey;

    private final BookRepository br;
    private final RestTemplate restTemplate;
    private final BookCopyRepository bcr;
    private final GenreRepository gr;

    public BookService(
            BookRepository repo,
            BookCopyRepository bcr, GenreRepository gr,
            @Value("${google.books.api.key}") String googleBooksApiKey) {
        this.br = repo;
        this.bcr = bcr;
        this.restTemplate = new RestTemplate();
        this.gr = gr;
        this.googleBooksApiKey = googleBooksApiKey;
    }

    public List<BookDTO> getAllBooks() {
        return br.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO addBook(BookDTO dto) {
        String normalizedIsbn = normalizeIsbn(dto.getISBN());
        dto.setISBN(normalizedIsbn);

        Book book = br.findByISBN(normalizedIsbn);

        if (book == null) {
            book = mapToEntity(dto);
            book = br.save(book);
        }

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setAvailability(Availability.AVAILABLE);
        bcr.save(copy);

        return mapToDTO(book);
    }

    private String normalizeIsbn(String isbn) {
        return isbn == null ? null : isbn.replace("-", "").replace(" ", "").trim();
    }

    private BookDTO mapToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setISBN(book.getISBN());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPages(book.getPages());
        dto.setImageUrl(book.getImageUrl());
        dto.setDescription(book.getDescription());
        dto.setReleaseYear(book.getReleaseYear());
        dto.setReviewScore(book.getReviewScore() != null ? book.getReviewScore() : 0.0);
        dto.setAddedDate(book.getAddedDate());
        dto.setGenres(
                book.getGenres().stream()
                        .map(Genre::getName)
                        .toList());
        return dto;
    }

    private Book mapToEntity(BookDTO dto) {
        Book book = new Book();
        book.setISBN(dto.getISBN());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());
        book.setGenres(mapGenres(dto.getGenres()));
        return book;
    }

    private List<Genre> mapGenres(List<String> genreNames) {
        if (genreNames == null) {
            return new ArrayList<>();
        }

        return genreNames.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .map(name -> gr.findById(name).orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setName(name);
                    genre.setColor("#808080");
                    genre.setDescription(null);
                    return gr.save(genre);
                }))
                .toList();
    }

    public BookDTO getBookById(Long id) {
        return br.findById(id)
                .map(book -> mapToDTO(book)) // Zet Book om naar BookDTO
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
    }

    public BookDTO fetchBookByISBN(String isbn) {
        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Geen boek gevonden voor ISBN: " + isbn);
        }

        Map<String, Object> bookData = (Map<String, Object>) response.get("ISBN:" + isbn);

        if (bookData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Geen boekdetails gevonden voor ISBN: " + isbn);
        }

        BookDTO dto = new BookDTO();
        dto.setISBN(isbn);
        dto.setTitle((String) bookData.get("title"));
        dto.setReleaseYear(extractYearFromOpenLibraryDate((String) bookData.get("publish_date")));

        List<Map<String, String>> authors = (List<Map<String, String>>) bookData.get("authors");
        if (authors != null && !authors.isEmpty()) {
            dto.setAuthor(authors.get(0).get("name"));
        }

        Object pages = bookData.get("number_of_pages");
        if (pages instanceof Integer) {
            dto.setPages((Integer) pages);
        }

        Map<String, String> cover = (Map<String, String>) bookData.get("cover");
        if (cover != null) {
            dto.setImageUrl(cover.get("medium"));
        }

        return dto;
    }

    public List<BookCandidateDTO> searchBookCandidates(String title, String author, Integer releaseYear) {
        List<Map<String, Object>> items = fetchGoogleBooksItems(title, author);
        List<BookCandidateDTO> candidates = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Map<String, Object> volumeInfo = (Map<String, Object>) item.get("volumeInfo");
            if (volumeInfo == null)
                continue;

            if (!matchesCandidate(volumeInfo, title, author, releaseYear))
                continue;

            String isbn13 = extractIsbn13(
                    (List<Map<String, String>>) volumeInfo.get("industryIdentifiers"));

            if (isbn13 == null)
                continue;

            candidates.add(mapToCandidateDTO(volumeInfo, isbn13));
        }

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Geen kandidaten gevonden voor title, author en releaseYear");
        }

        return candidates;
    }

    private Integer extractYearFromOpenLibraryDate(String rawDate) {
        if (rawDate == null)
            return 0;

        java.util.regex.Matcher matcher = Pattern.compile("\\d{4}").matcher(rawDate);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }

        return 0;
    }

    private List<Map<String, Object>> fetchGoogleBooksItems(String title, String author) {
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedAuthor = URLEncoder.encode(author, StandardCharsets.UTF_8);

        String url = "https://www.googleapis.com/books/v1/volumes?q=intitle:"
                + encodedTitle
                + "+inauthor:"
                + encodedAuthor
                + "&printType=books"
                + "&maxResults=20"
                + "&key=" + googleBooksApiKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !response.containsKey("items")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Geen boeken gevonden voor title en author");
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Geen boeken gevonden voor title en author");
        }

        return items;
    }

    private BookCandidateDTO mapToCandidateDTO(Map<String, Object> volumeInfo, String isbn13) {
        BookCandidateDTO dto = new BookCandidateDTO();
        dto.setTitle((String) volumeInfo.get("title"));
        dto.setAuthor(getFirstAuthor(volumeInfo));
        dto.setReleaseYear(extractYearFromGoogleDate((String) volumeInfo.get("publishedDate")));
        dto.setIsbn13(isbn13);

        Map<String, String> imageLinks = (Map<String, String>) volumeInfo.get("imageLinks");
        if (imageLinks != null) {
            dto.setImageUrl(imageLinks.get("thumbnail"));
        }

        return dto;
    }

    private boolean matchesCandidate(Map<String, Object> volumeInfo, String title, String author, Integer releaseYear) {
        return matchesTitle(volumeInfo, title)
                && matchesAuthor(volumeInfo, author)
                && matchesReleaseYear(volumeInfo, releaseYear);
    }

    private String normalize(String value) {
        if (value == null)
            return "";
        return value.toLowerCase().trim();
    }

    private boolean matchesTitle(Map<String, Object> volumeInfo, String expectedTitle) {
        String foundTitle = normalize((String) volumeInfo.get("title"));
        String wantedTitle = normalize(expectedTitle);

        return foundTitle.equals(wantedTitle) || foundTitle.contains(wantedTitle);
    }

    private boolean matchesAuthor(Map<String, Object> volumeInfo, String expectedAuthor) {
        List<String> authors = (List<String>) volumeInfo.get("authors");

        if (authors == null || authors.isEmpty()) {
            return false;
        }

        String wantedAuthor = normalize(expectedAuthor);

        for (String author : authors) {
            if (normalize(author).equals(wantedAuthor)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesReleaseYear(Map<String, Object> volumeInfo, Integer expectedYear) {
        if (expectedYear == null)
            return true;

        String publishedDate = (String) volumeInfo.get("publishedDate");
        if (publishedDate == null || publishedDate.length() < 4) {
            return false;
        }
        publishedDate = publishedDate.trim();

        try {
            int year = Integer.parseInt(publishedDate.substring(0, 4));
            return year == expectedYear;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getFirstAuthor(Map<String, Object> volumeInfo) {
        List<String> authors = (List<String>) volumeInfo.get("authors");
        if (authors == null || authors.isEmpty()) {
            return "";
        }
        return authors.get(0);
    }

    private Integer extractYearFromGoogleDate(String publishedDate) {
        if (publishedDate == null)
            return null;

        publishedDate = publishedDate.trim();
        if (publishedDate.length() < 4)
            return null;

        try {
            return Integer.parseInt(publishedDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractIsbn13(List<Map<String, String>> identifiers) {
        if (identifiers == null)
            return null;

        for (Map<String, String> identifier : identifiers) {
            if ("ISBN_13".equalsIgnoreCase(identifier.get("type"))) {
                return identifier.get("identifier");
            }
        }

        return null;
    }

    public Integer getMinReleaseYear() {
        Integer min = br.findMinReleaseYear();
        return min != null ? min : 1900;
    }

    public Integer getMaxReleaseYear() {
        Integer max = br.findMaxReleaseYear();
        int year = LocalDate.now().getYear();
        return max != null ? max : year;
    }

    public Integer getMaxPages() {
        Integer max = br.findMaxPages();
        return max != null ? max : 1500;
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
}