package edu.ap.bib_backend.jpa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Formula;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String isbn;
    private String title;
    private String author;
    private int pages;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "release_year")
    private int releaseYear;

    @Formula("(SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.book_id = id)")
    private Double reviewScore;

    @Column(name = "added_date")
    private LocalDateTime addedDate;

    @Formula("(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = id)")
    private Integer totalCopies;

    @Formula("""
            (
                SELECT COUNT(*)
                FROM book_copies bc
                WHERE bc.book_id = id
                  AND NOT EXISTS (
                      SELECT 1
                      FROM loan_copies lc
                      JOIN loans l ON l.id = lc.loan_id
                      WHERE lc.book_copy_id = bc.id
                        AND l.returned_date IS NULL
                  )
            )
            """)
    private Integer availableCopies;

    @PrePersist
    protected void onCreate() {
        addedDate = LocalDateTime.now();
    }

    public Book() {
    }

    public Book(String isbn, String title, String author, int pages, String image_url, String description,
            int release_year) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.imageUrl = image_url;
        this.description = description;
        this.releaseYear = release_year;
    }

    @ManyToMany
    @JoinTable(name = "book_genres", joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "book_id", "genre_id" }))
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "book")
    private List<BookCopy> bookCopies = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "reading_level_id")
    private ReadingLevel readingLevel;

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", ISBN='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", pages=" + pages +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", releaseYear=" + releaseYear +
                '}';
    }
}
