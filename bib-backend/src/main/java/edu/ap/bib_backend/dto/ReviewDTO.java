package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {

    private Long id;
    private String comment;
    private int rating;
    private int reportCount;

    public ReviewDTO() {
    }

    public ReviewDTO(Long id, String comment, int rating, int reportCount) {
        this.id = id;
        this.comment = comment;
        this.rating = rating;
        this.reportCount = reportCount;
    }
}