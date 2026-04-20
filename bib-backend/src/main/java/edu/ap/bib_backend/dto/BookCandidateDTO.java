package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookCandidateDTO {
    private Long id;
    private String title;
    private String author;
    private String isbn13;
    private Integer releaseYear;
    private String imageUrl;

    public BookCandidateDTO() {
    }
}
