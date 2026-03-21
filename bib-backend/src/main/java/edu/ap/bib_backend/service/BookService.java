package edu.ap.bib_backend.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.LabelRepository;

@Service
public class BookService {

    private final BookRepository br;
    private final LabelRepository lr;

    public BookService(BookRepository repo, LabelRepository labelRepository) {
        br = repo;
        lr = labelRepository;
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
        dto.setDescription(book.getDescription());
        dto.setReleaseYear(book.getReleaseYear());
        dto.setReviewScore(book.getReviewScore() != null ? book.getReviewScore() : 0.0);
        dto.setLabels(book.getLabels().stream().map(Label::getName).toList());
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
        RestTemplate restTemplate = createRestTemplate();

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

        return dto;
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

    public List<BookDTO> getRelatedBooks(Long bookId, int limit) {
        br.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + bookId));

        int safeLimit = Math.max(1, limit);
        List<Book> relatedBooks = br.findRelatedBooks(bookId, PageRequest.of(0, safeLimit));
        return mapToDTOList(relatedBooks);
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

    public BookDTO assignLabels(Long bookId, List<String> labels) {
        Book book = br.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + bookId));

        Set<String> requestedLabels = labels == null
                ? Set.of()
                : labels.stream()
                        .filter(label -> label != null && !label.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Label> labelEntities = requestedLabels.isEmpty()
                ? List.of()
                : lr.findAllById(requestedLabels);

        if (labelEntities.size() != requestedLabels.size()) {
            Set<String> foundNames = labelEntities.stream().map(Label::getName).collect(Collectors.toSet());
            List<String> missing = requestedLabels.stream().filter(label -> !foundNames.contains(label)).toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Onbekende labels: " + String.join(", ", missing));
        }

        book.setLabels(new ArrayList<>(labelEntities));
        Book saved = br.save(book);
        return mapToDTO(saved);
    }

    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = br.findById(id)
                .orElseThrow(() -> new RuntimeException("Boek niet gevonden met id: " + id));

        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setISBN(dto.getISBN());
        book.setPages(dto.getPages());
        book.setImageUrl(dto.getImageUrl());
        book.setDescription(dto.getDescription());
        book.setReleaseYear(dto.getReleaseYear());

        Book updatedBook = br.save(book);
        return mapToDTO(updatedBook);
    }

    public void deleteBook(Long id) {
        if (!br.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Boek niet gevonden met id: " + id);
        }

        br.deleteById(id);
    }

    public BookDTO fetchBookByIdWithFallback(Long id) {
        BookDTO dto = getBookById(id);

        if (dto.getImageUrl() == null || dto.getImageUrl().isBlank()) {
            Book book = br.findById(id).orElseThrow();
            String isbn = book.getISBN();
            if (isbn != null) {
                BookDTO fetchedByIsbn = fetchBookByISBN(isbn);
                if (fetchedByIsbn.getImageUrl() != null) {
                    dto.setImageUrl(fetchedByIsbn.getImageUrl());
                }
            }
        }

        return dto;
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
}
