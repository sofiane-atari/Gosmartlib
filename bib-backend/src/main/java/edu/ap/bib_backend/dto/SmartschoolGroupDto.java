package edu.ap.bib_backend.dto;

import java.util.List;

public record SmartschoolGroupDto(
    String primaryGroupName,
    List<String> parentGroupNames
) {}