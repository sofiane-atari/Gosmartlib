package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {

    private Long id;
    private String comment;
    private int rating;
    private int reportCount;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;

    public ReviewDTO() {
    }

    public ReviewDTO(Long id, String comment, int rating, int reportCount, Long bookId, String bookTitle,
            String bookAuthor) {
        this.id = id;
        this.comment = comment;
        this.rating = rating;
        this.reportCount = reportCount;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
    }
}