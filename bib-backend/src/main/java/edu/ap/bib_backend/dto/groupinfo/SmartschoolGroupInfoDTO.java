package edu.ap.bib_backend.dto.groupinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SmartschoolGroupInfoDTO(
        @JsonProperty("groups") List<GroupInfoItemDTO> groups,
        @JsonProperty("parentGroups") List<GroupInfoItemDTO> parentGroups) {
}