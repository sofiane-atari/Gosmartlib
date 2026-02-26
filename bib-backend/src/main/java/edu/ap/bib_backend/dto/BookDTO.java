package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDTO {

    private Long id;

    private Long ISBN;
    private String title;
    private String author;
    private int pages;
    private String image_url;

    public BookDTO() {
    }

    public BookDTO(Long id, Long ISBN, String title, String author, int pages, String image_url) {
        this.id = id;
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.image_url = image_url;
    }

    // Getters & Setters
}