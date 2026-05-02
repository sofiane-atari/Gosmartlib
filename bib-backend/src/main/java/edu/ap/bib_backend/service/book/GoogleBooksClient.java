package edu.ap.bib_backend.service.book;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.mapper.BookCandidateMapper;
import org.springframework.beans.factory.annotation.Value;

@Service
public class GoogleBooksClient {

    private final RestTemplate restTemplate;
    private final String googleBooksApiKey;
    private final BookCandidateMapper bookCandidateMapper;

    public GoogleBooksClient(
            RestTemplate restTemplate,
            @Value("${google.books.api.key}") String googleBooksApiKey,
            BookCandidateMapper bookCandidateMapper) {
        this.restTemplate = restTemplate;
        this.googleBooksApiKey = googleBooksApiKey;
        this.bookCandidateMapper = bookCandidateMapper;
    }

    private List<Map<String, Object>> fetchGoogleBooksItems(String title, String author) {
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedAuthor = URLEncoder.encode(author, StandardCharsets.UTF_8);

        String url = "https://www.googleapis.com/books/v1/volumes?q=intitle:"
                + encodedTitle
                + "+inauthor:"
                + encodedAuthor
                + "&printType=books"
                + "&langRestrict=nl"
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

    public List<BookCandidateDTO> searchBookCandidates(String title, String author, Integer releaseYear) {
        List<Map<String, Object>> items = fetchGoogleBooksItems(title, author);
        List<BookCandidateDTO> candidates = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Map<String, Object> volumeInfo = (Map<String, Object>) item.get("volumeInfo");
            if (volumeInfo == null) {
                continue;
            }

            if (!matchesCandidate(volumeInfo, title, author, releaseYear)) {
                continue;
            }

            String isbn13 = extractIsbn13(
                    (List<Map<String, String>>) volumeInfo.get("industryIdentifiers"));

            if (isbn13 == null) {
                continue;
            }

            candidates.add(bookCandidateMapper.toCandidateDTO(volumeInfo, isbn13));
        }

        if (releaseYear != null) {
            candidates.sort((a, b) -> {
                Integer yearA = a.getReleaseYear();
                Integer yearB = b.getReleaseYear();

                int distanceA = yearA == null ? Integer.MAX_VALUE : Math.abs(yearA - releaseYear);
                int distanceB = yearB == null ? Integer.MAX_VALUE : Math.abs(yearB - releaseYear);

                return Integer.compare(distanceA, distanceB);
            });
        }
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Geen kandidaten gevonden voor title, author en releaseYear");
        }

        return candidates.stream().limit(4).toList();
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

    private boolean matchesCandidate(Map<String, Object> volumeInfo, String title, String author, Integer releaseYear) {
        return isDutchBook(volumeInfo)
                && matchesTitle(volumeInfo, title)
                && matchesAuthor(volumeInfo, author);
    }

    private boolean matchesTitle(Map<String, Object> volumeInfo, String expectedTitle) {
        String foundTitle = normalize((String) volumeInfo.get("title"));
        String wantedTitle = normalize(expectedTitle);

        return foundTitle.equals(wantedTitle)
                || foundTitle.contains(wantedTitle);
    }

    private boolean matchesAuthor(Map<String, Object> volumeInfo, String expectedAuthor) {
        List<String> authors = (List<String>) volumeInfo.get("authors");

        if (authors == null || authors.isEmpty()) {
            return false;
        }

        String wantedAuthor = normalize(expectedAuthor);

        for (String author : authors) {
            String foundAuthor = normalize(author);

            if (foundAuthor.equals(wantedAuthor)
                    || foundAuthor.contains(wantedAuthor)
                    || wantedAuthor.contains(foundAuthor)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesReleaseYear(Map<String, Object> volumeInfo, Integer expectedYear) {
        if (expectedYear == null) {
            return true;
        }

        String publishedDate = (String) volumeInfo.get("publishedDate");
        if (publishedDate == null || publishedDate.length() < 4) {
            return false;
        }

        try {
            int year = Integer.parseInt(publishedDate.substring(0, 4));
            return Math.abs(year - expectedYear) <= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private boolean isDutchBook(Map<String, Object> volumeInfo) {
        Object language = volumeInfo.get("language");

        if (!(language instanceof String lang)) {
            return false;
        }

        return "nl".equalsIgnoreCase(lang);
    }
}
