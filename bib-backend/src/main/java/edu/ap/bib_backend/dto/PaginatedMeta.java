package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginatedMeta(
        @JsonProperty("currentPage") int currentPage,

        @JsonProperty("pageSize") int pageSize,

        @JsonProperty("totalItems") long totalItems,

        @JsonProperty("totalPages") int totalPages) {

}
