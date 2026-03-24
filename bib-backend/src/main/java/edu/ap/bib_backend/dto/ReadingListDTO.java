package edu.ap.bib_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;

public record ReadingListDTO(
                @JsonProperty("name") String name,
                @JsonProperty("targetType") ReadingListTargetType targetType,
                @JsonProperty("targetValue") String targetValue) {
}
