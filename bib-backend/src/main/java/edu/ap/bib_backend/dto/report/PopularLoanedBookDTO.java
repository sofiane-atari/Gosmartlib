package edu.ap.bib_backend.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PopularLoanedBookDTO(
        @JsonProperty("bookId") Long bookId,
        @JsonProperty("title") String title,
        @JsonProperty("author") String author,
        @JsonProperty("imageUrl") String imageUrl,
        @JsonProperty("loanCount") long loanCount) {
}
