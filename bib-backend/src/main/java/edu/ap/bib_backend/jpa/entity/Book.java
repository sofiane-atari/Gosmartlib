package edu.ap.bib_backend.jpa.entity;

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

    private Long ISBN;
    private String title;
    private String author;
    private int pages;
    private String image_url;
    private String description;
    private int release_year;
    private int copies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    public Book() {
    }

    public Book(Long ISBN, String title, String author, int pages, String image_url, String description,
            int release_year, Availability availability, int copies) {
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.image_url = image_url;
        this.description = description;
        this.release_year = release_year;
        this.availability = availability;
        this.copies = copies;
    }
}