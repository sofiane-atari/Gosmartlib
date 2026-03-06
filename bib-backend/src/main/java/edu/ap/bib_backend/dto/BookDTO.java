package edu.ap.bib_backend.dto;

import edu.ap.bib_backend.jpa.entity.Availability;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDTO {

    private Long id;

    private String ISBN;
    private String title;
    private String author;
    private int pages;
    private String image_url;
    private String description;
    private int release_year;
    private Availability availability;
    private int copies;

    public BookDTO() {
    }

    public BookDTO(Long id, String ISBN, String title, String author, int pages, String image_url, String description,
            int release_year, Availability availability, int copies) {
        this.id = id;
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