package edu.ap.bib_backend.service;

import java.nio.charset.StandardCharsets;

import java.net.URI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.dto.BookPreviewDTO;

import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.jpa.specification.BookSpecification;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.net.URLEncoder;

@Service
@Transactional
public class BookService {

    private String googleBooksApiKey;

    private final BookRepository br;
    private final RestTemplate restTemplate;
    private final BookCopyRepository bcr;
    private final GenreRepository gr;
    private final ReviewRepository reviewRepository;
    private final BookLoanRepository bookLoanRepository;
    private final ReadingLevelRepository rlr;

    public BookService(
            BookRepository repo,
            BookCopyRepository bcr,
            GenreRepository gr,
            ReviewRepository reviewRepository,
            BookLoanRepository bookLoanRepository,
            ReadingLevelRepository rlr,
            RestTemplate restTemplate,
            @Value("${google.books.api.key}") String googleBooksApiKey) {
        this.br = repo;
        this.bcr = bcr;
        this.gr = gr;
        this.reviewRepository = reviewRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.restTemplate = restTemplate;
        this.rlr = rlr;
        this.googleBooksApiKey = googleBooksApiKey;
    }

    @Transactional
    public List<BookDTO> getAllBooks() {
        return br.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO addBook(BookDTO dto) {
        String normalizedIsbn = normalizeIsbn(dto.getIsbn());
        dto.setIsbn(normalizedIsbn);

        Book book = null;

        if (normalizedIsbn != null && !normalizedIsbn.isBlank()) {
            book = br.findByIsbn(normalizedIsbn);
        }

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

    @Transactional
    public void deleteBook(Long id) {
        Book book = br.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("id", "Boek niet gevonden met id: " + id));

        boolean hasBorrowedCopies = bcr.existsByBookIdAndAvailability(id, Availability.BORROWED);

        if (hasBorrowedCopies) {
            throw new IllegalStateException(
                    "Dit boek kan niet verwijderd worden omdat minstens één exemplaar momenteel is uitgeleend.");
        }

        reviewRepository.deleteByBookId(id);
        bookLoanRepository.deleteByCopiesBookId(id);
        bcr.deleteByBookId(id);

        book.getGenres().clear();
        br.delete(book);
    }

    @Transactional
    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = br.findById(id)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));

        String normalizedIsbn = normalizeIsbn(dto.getIsbn());

        if (normalizedIsbn != null && !normalizedIsbn.isBlank()) {
            Book existingBookWithIsbn = br.findByIsbn(normalizedIsbn);

            if (existingBookWithIsbn != null && !existingBookWithIsbn.getId().equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Er bestaat al een ander boek met ISBN: " + normalizedIsbn);
            }
        }

        book.setIsbn(normalizedIsbn);
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());
        book.setGenres(mapGenresFromDTO(dto.getGenres()));

        Book updatedBook = br.save(book);
        return mapToDTO(updatedBook);
    }

    private String normalizeIsbn(String isbn) {
        return isbn == null ? null : isbn.replace("-", "").replace(" ", "").trim();
    }

    private BookDTO mapToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setIsbn(book.getIsbn());
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
                        .map(this::mapGenreToDTO)
                        .toList());

        dto.setTotalCopies(book.getBookCopies().size());
        dto.setAvailableCopies((int) book.getBookCopies().stream()
                .filter(copy -> copy.getAvailability() == Availability.AVAILABLE)
                .count());

        if (book.getReadingLevel() != null) {
            dto.setReadingLevel(new ReadingLevelDTO(
                    book.getReadingLevel().getId(),
                    book.getReadingLevel().getNiveauName(),
                    book.getReadingLevel().getDescription()));
        }
        return dto;
    }

    private GenreDTO mapGenreToDTO(Genre genre) {
        return GenreDTO.builder()
                .name(genre.getName())
                .color(genre.getColor())
                .description(genre.getDescription())
                .bookCount(null)
                .build();
    }

    private Book mapToEntity(BookDTO dto) {
        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());
        book.setGenres(mapGenresFromDTO(dto.getGenres()));

        if (dto.getReadingLevel() != null && dto.getReadingLevel().getId() != null) {
            ReadingLevel readingLevel = rlr
                    .findById(dto.getReadingLevel().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Leesniveau niet gevonden"));

            book.setReadingLevel(readingLevel);
        }
        return book;
    }

    private Set<Genre> mapGenresFromDTO(List<GenreDTO> genresDto) {
        Set<Genre> genres = new HashSet<>();

        if (genresDto == null) {
            return genres;
        }

        for (GenreDTO genreDto : genresDto) {
            Genre genre = gr.findByName(genreDto.name())
                    .orElseThrow(() -> new IllegalArgumentException("Genre bestaat niet: " + genreDto.name()));
            genres.add(genre);
        }

        return genres;
    }

    @Transactional
    public BookDTO getBookById(Long id) {
        return br.findById(id)
                .map(book -> mapToDTO(book)) // Zet Book om naar BookDTO
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));
    }

    public Book getBookById(Long id, boolean returnEntity) {
        return br.findById(id)
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
        dto.setIsbn(isbn);
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

    public Integer getMaxPages() {
        Integer max = br.findMaxPages();
        return max != null ? max : 1500;
    }

    public Integer getMinPages() {
        Integer min = br.findMinPages();
        return min != null ? min : 0;
    }

    @Transactional
    public List<BookDTO> getMostReadBooks(int limit) {
        List<Book> books = br.findMostReadBook(PageRequest.of(0, limit));

        if (books.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen uitgeleende boeken gevonden");
        }

        return mapToDTOList(books);
    }

    @Transactional
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

    public List<BookDTO> getRelatedBooksCarousel(Long bookId, int limit) {
        br.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + bookId));

        int safeLimit = Math.max(1, Math.min(limit, 50)); // Max 50 voor performance
        List<Object[]> results = br.findRelatedBooksWithScore(bookId, PageRequest.of(0, safeLimit));

        return results.stream()
                .map(row -> (Book) row[0])
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BookPreviewDTO fetchPreviewByISBN(String rawIsbn) {
        String normalizedIsbn = normalizeAndValidateIsbn(rawIsbn);

        RestTemplate restTemplate = createRestTemplate();
        String requestUrl = UriComponentsBuilder
                .fromUriString("https://openlibrary.org/api/books")
                .queryParam("bibkeys", "ISBN:" + normalizedIsbn)
                .queryParam("format", "json")
                .queryParam("jscmd", "data")
                .build()
                .toUriString();

        Map<String, Object> response;
        try {
            response = restTemplate.getForObject(requestUrl, Map.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preview tijdelijk niet beschikbaar");
        }

        if (response == null || response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen boek gevonden voor dit ISBN");
        }

        Object rawBookData = response.get("ISBN:" + normalizedIsbn);
        if (!(rawBookData instanceof Map<?, ?> bookData) || bookData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geen boek gevonden voor dit ISBN");
        }

        String detailsUrl = toAbsoluteOpenLibraryUrl(extractString(bookData, "url"));
        if (!isAllowedExternalUrl(detailsUrl)) {
            detailsUrl = "https://openlibrary.org/isbn/" + normalizedIsbn;
        }

        String previewUrl = firstNonBlank(
                extractString(bookData, "preview_url"),
                extractPreviewUrlFromEbooks(bookData));

        if (!isAllowedExternalUrl(previewUrl)) {
            previewUrl = null;
        }

        String embedUrl = firstNonBlank(
                buildEmbedUrl(bookData),
                buildEmbedUrlFromArchiveUrl(previewUrl),
                buildEmbedUrlFromArchiveUrl(detailsUrl));
        boolean previewAvailable = previewUrl != null || embedUrl != null;
        String message = previewAvailable
                ? "Preview beschikbaar"
                : "Geen preview beschikbaar";

        return new BookPreviewDTO(previewAvailable, previewUrl, detailsUrl, message, embedUrl);
    }

    private String normalizeAndValidateIsbn(String rawIsbn) {
        if (rawIsbn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN is verplicht");
        }

        String normalized = rawIsbn.replaceAll("[-\\s]", "").trim().toUpperCase();
        boolean validFormat = normalized.matches("^[0-9X]{10}$") || normalized.matches("^[0-9]{13}$");

        if (!validFormat) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ongeldig ISBN-formaat");
        }

        return normalized;
    }

    private String extractPreviewUrlFromEbooks(Map<?, ?> bookData) {
        Object ebooksRaw = bookData.get("ebooks");
        if (!(ebooksRaw instanceof List<?> ebooks) || ebooks.isEmpty()) {
            return null;
        }

        Object firstEbook = ebooks.get(0);
        if (!(firstEbook instanceof Map<?, ?> ebookMap)) {
            return null;
        }

        Object previewUrlRaw = ebookMap.get("preview_url");
        return previewUrlRaw instanceof String ? (String) previewUrlRaw : null;
    }

    private String extractString(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private String toAbsoluteOpenLibraryUrl(String maybeRelativeUrl) {
        if (maybeRelativeUrl == null || maybeRelativeUrl.isBlank()) {
            return null;
        }

        if (maybeRelativeUrl.startsWith("/")) {
            return "https://openlibrary.org" + maybeRelativeUrl;
        }

        return maybeRelativeUrl;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isAllowedExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (host == null || !"https".equalsIgnoreCase(scheme)) {
            return false;
        }

        String normalizedHost = host.toLowerCase();
        return normalizedHost.equals("openlibrary.org")
                || normalizedHost.endsWith(".openlibrary.org")
                || normalizedHost.equals("archive.org")
                || normalizedHost.endsWith(".archive.org");
    }

    private String buildEmbedUrl(Map<?, ?> bookData) {
        String ocaid = extractArchiveOcaid(bookData);
        if (ocaid == null) {
            return null;
        }

        String embedUrl = "https://archive.org/embed/" + ocaid;
        return isAllowedExternalUrl(embedUrl) ? embedUrl : null;
    }

    private String buildEmbedUrlFromArchiveUrl(String sourceUrl) {
        if (!isAllowedExternalUrl(sourceUrl)) {
            return null;
        }

        URI uri;
        try {
            uri = URI.create(sourceUrl);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String prefix = "/details/";
        int detailsStart = path.indexOf(prefix);
        if (detailsStart < 0) {
            return null;
        }

        String remainder = path.substring(detailsStart + prefix.length());
        String[] segments = remainder.split("/");
        if (segments.length == 0 || segments[0].isBlank()) {
            return null;
        }

        String archiveId = sanitizeOcaid(segments[0]);
        if (archiveId == null) {
            return null;
        }

        String embedUrl = "https://archive.org/embed/" + archiveId;
        return isAllowedExternalUrl(embedUrl) ? embedUrl : null;
    }

    private String extractArchiveOcaid(Map<?, ?> bookData) {
        String directOcaid = sanitizeOcaid(extractString(bookData, "ocaid"));
        if (directOcaid != null) {
            return directOcaid;
        }

        Object identifiersRaw = bookData.get("identifiers");
        if (!(identifiersRaw instanceof Map<?, ?> identifiers)) {
            return null;
        }

        Object ocaidRaw = identifiers.get("ocaid");
        if (ocaidRaw instanceof String ocaidString) {
            return sanitizeOcaid(ocaidString);
        }

        if (ocaidRaw instanceof List<?> ocaidList && !ocaidList.isEmpty()) {
            Object first = ocaidList.get(0);
            if (first instanceof String firstOcaid) {
                return sanitizeOcaid(firstOcaid);
            }
        }

        return null;
    }

    private String sanitizeOcaid(String rawOcaid) {
        if (rawOcaid == null || rawOcaid.isBlank()) {
            return null;
        }

        String trimmed = rawOcaid.trim();
        return trimmed.matches("^[A-Za-z0-9._-]{1,120}$") ? trimmed : null;
    }

    protected RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    public Page<BookDTO> getFilteredBooks(
            String searchTerm,
            BookAvailabilityFilter availabilityFilter,
            Integer minPages,
            Integer maxPages,
            int page,
            int size,
            BookSortOption sortBy) {
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
                maxPages);

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
            case AUTHOR_AZ -> Sort.by("author").ascending();
            case AUTHOR_ZA -> Sort.by("author").descending();
        };
    }
}
