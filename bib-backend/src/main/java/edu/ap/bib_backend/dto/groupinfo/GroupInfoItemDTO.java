package edu.ap.bib_backend.dto.groupinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GroupInfoItemDTO(
        @JsonProperty("groupID") String groupID,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("platform") String platform,
        @JsonProperty("instituteNumber") Integer instituteNumber,
        @JsonProperty("adminNumber") Integer adminNumber) {
}