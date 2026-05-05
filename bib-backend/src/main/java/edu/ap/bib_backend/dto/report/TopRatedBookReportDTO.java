package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TopRatedBookReportDTO(
        @JsonProperty("bookId") Long bookId,
        @JsonProperty("title") String title,
        @JsonProperty("author") String author,
        @JsonProperty("imageUrl") String imageUrl,
        @JsonProperty("averageRating") double averageRating,
        @JsonProperty("reviewCount") long reviewCount) {
}