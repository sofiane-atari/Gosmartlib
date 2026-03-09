package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 1000)
    private String comment;
    private int rating;
    private int reportCount = 0;

    // Obsolete field kept here to satisfy existing database column
    // constraint, preventing Field 'is_reported' doesn't have a default value
    // exceptions
    private boolean isReported = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public Review() {
    }

    public Review(String comment, int rating, Book book) {
        this.comment = comment;
        this.rating = rating;
        this.book = book;
        this.reportCount = 0;
    }
}