package edu.ap.bib_backend.jpa.entity.review;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review {

    private static final int REPORT_THRESHOLD = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private int reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.ACTIVE;

    private LocalDateTime flaggedAt;

    private LocalDateTime moderatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReport> reports = new ArrayList<>();

    public Review() {
    }

    public Review(String comment, int rating, Book book, User user) {
        this.comment = comment;
        this.rating = rating;
        this.book = book;
        this.user = user;
        this.reportCount = 0;
        this.moderationStatus = ReviewModerationStatus.ACTIVE;
    }

    public void updateReportStatus(int uniqueReportCount) {
        this.reportCount = uniqueReportCount;

        boolean canBeFlagged = this.moderationStatus == ReviewModerationStatus.ACTIVE
                || this.moderationStatus == ReviewModerationStatus.KEPT;

        if (uniqueReportCount >= REPORT_THRESHOLD && canBeFlagged) {
            this.moderationStatus = ReviewModerationStatus.FLAGGED;
            this.flaggedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void markAsKept() {
        this.moderationStatus = ReviewModerationStatus.KEPT;
        this.reportCount = 0;
        this.flaggedAt = null;
        this.moderatedAt = LocalDateTime.now();
        this.reports.clear();
    }

    public boolean isFlagged() {
        return this.moderationStatus == ReviewModerationStatus.FLAGGED;
    }
}