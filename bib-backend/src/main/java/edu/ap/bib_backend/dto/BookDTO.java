package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDTO {

    private Long id;

    private String isbn;
    private String title;
    private String author;
    private int pages;
    private String imageUrl;
    private String description;
    private int releaseYear;
    private double reviewScore;
    private LocalDateTime addedDate;

    private int totalCopies;
    private int availableCopies;

    private List<GenreDTO> genres = new ArrayList<>();

    public BookDTO() {
    }

    public BookDTO(Long id, String isbn, String title, String author, int pages, String image_url, String description,
            int release_year, double reviewScore, LocalDateTime addedDate, List<GenreDTO> genres) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.imageUrl = image_url;
        this.description = description;
        this.releaseYear = release_year;
        this.reviewScore = reviewScore;
        this.addedDate = addedDate;
        this.genres = genres;
    }

    @Override
    public String toString() {
        return "BookDTO{" +
                "id=" + id +
                ", ISBN='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", pages=" + pages +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", releaseYear=" + releaseYear +
                ", reviewScore=" + reviewScore +
                ", genres=" + genres +
                '}';
    }
}