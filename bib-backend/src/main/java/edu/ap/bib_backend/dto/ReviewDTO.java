package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
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
    private ReviewModerationStatus moderationStatus;
    private boolean reportedByCurrentUser;
    private LocalDateTime createdAt;
    private String bookImageUrl;

    public ReviewDTO() {
    }

    public ReviewDTO(
            Long id,
            String comment,
            int rating,
            int reportCount,
            Long bookId,
            String bookTitle,
            String bookAuthor,
            String bookImageUrl,
            ReviewModerationStatus moderationStatus,
            boolean reportedByCurrentUser,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.comment = comment;
        this.rating = rating;
        this.reportCount = reportCount;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.bookImageUrl = bookImageUrl;
        this.moderationStatus = moderationStatus;
        this.reportedByCurrentUser = reportedByCurrentUser;
        this.createdAt = createdAt;
    }
}