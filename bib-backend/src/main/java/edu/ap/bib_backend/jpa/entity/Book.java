package edu.ap.bib_backend.jpa.entity;

import org.hibernate.annotations.Formula;
import java.util.ArrayList;
import java.util.List;

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

    private String ISBN;
    private String title;
    private String author;
    private int pages;
    private String description;
    private int copies;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "release_year")
    private int releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    @Formula("(SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.book_id = id)")
    private Double reviewScore;

    public Book() {
    }

    public Book(String ISBN, String title, String author, int pages, String image_url, String description,
            int release_year, Availability availability, int copies) {
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.imageUrl = image_url;
        this.description = description;
        this.releaseYear = release_year;
        this.availability = availability;
        this.copies = copies;
    }

    @ManyToMany
    @JoinTable(name = "book_labels", joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name = "label_name"))
    private List<Label> labels = new ArrayList<>();


    @OneToMany(mappedBy = "book")
    private List<BookCopy> bookCopies= new ArrayList<>();


    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", ISBN='" + ISBN + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", pages=" + pages +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", releaseYear=" + releaseYear +
                ", availability=" + availability +
                ", copies=" + copies +
                '}';
    }
}