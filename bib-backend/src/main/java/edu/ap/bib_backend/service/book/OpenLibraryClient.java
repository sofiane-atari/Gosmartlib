package edu.ap.bib_backend.service.book;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.util.IsbnUtils;

@Service
public class OpenLibraryClient {

    private final RestTemplate restTemplate;

    public OpenLibraryClient(
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public BookPreviewDTO fetchPreviewByISBN(String rawIsbn) {
        String normalizedIsbn = IsbnUtils.normalizeAndValidateIsbn(rawIsbn);
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

    private String buildEmbedUrl(Map<?, ?> bookData) {
        String ocaid = extractArchiveOcaid(bookData);
        if (ocaid == null) {
            return null;
        }

        String embedUrl = "https://archive.org/embed/" + ocaid;
        return isAllowedExternalUrl(embedUrl) ? embedUrl : null;
    }

    private String sanitizeOcaid(String rawOcaid) {
        if (rawOcaid == null || rawOcaid.isBlank()) {
            return null;
        }

        String trimmed = rawOcaid.trim();
        return trimmed.matches("^[A-Za-z0-9._-]{1,120}$") ? trimmed : null;
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

    private String toAbsoluteOpenLibraryUrl(String maybeRelativeUrl) {
        if (maybeRelativeUrl == null || maybeRelativeUrl.isBlank()) {
            return null;
        }

        if (maybeRelativeUrl.startsWith("/")) {
            return "https://openlibrary.org" + maybeRelativeUrl;
        }

        return maybeRelativeUrl;
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

    private Integer extractYearFromOpenLibraryDate(String rawDate) {
        if (rawDate == null)
            return 0;

        java.util.regex.Matcher matcher = Pattern.compile("\\d{4}").matcher(rawDate);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }

        return 0;
    }

    private String extractString(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
