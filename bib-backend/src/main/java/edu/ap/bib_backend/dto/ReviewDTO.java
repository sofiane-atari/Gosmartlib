package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {

    private Long id;
    private String comment;
    private int rating;

    public ReviewDTO() {
    }

    public ReviewDTO(Long id,String comment, int rating) {
        this.id = id;
        this.comment = comment;
        this.rating = rating;
    }
}