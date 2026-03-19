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

    private String ISBN;
    private String title;
    private String author;
    private int pages;
    private String imageUrl;
    private String description;
    private int releaseYear;
    private double reviewScore;
    private LocalDateTime addedDate;

    private List<String> genres = new ArrayList<>();

    public BookDTO() {
    }

    public BookDTO(Long id, String ISBN, String title, String author, int pages, String image_url, String description,
            int release_year, double reviewScore, LocalDateTime addedDate, List<String> genres) {
        this.id = id;
        this.ISBN = ISBN;
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
                ", ISBN='" + ISBN + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", pages=" + pages +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", releaseYear=" + releaseYear +
                ", reviewScore=" + reviewScore +
                '}';
    }
}