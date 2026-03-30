package edu.ap.bib_backend.mapper;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import edu.ap.bib_backend.dto.BookCandidateDTO;

@Component
public class BookCandidateMapper {

    public BookCandidateDTO toCandidateDTO(Map<String, Object> volumeInfo, String isbn13) {
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

}
