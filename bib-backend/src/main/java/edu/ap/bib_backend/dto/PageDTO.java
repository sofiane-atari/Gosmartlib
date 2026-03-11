package edu.ap.bib_backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PageDTO<T>(
        @JsonProperty("data") List<T> data,
        @JsonProperty("meta") PaginatedMeta meta) {
}