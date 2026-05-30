package edu.ap.bib_backend.dto;

public record CatalogSidebarSettingsDTO(
    boolean catalogFeaturedListsEnabled,
    boolean catalogMostReadEnabled,
    boolean catalogNewestEnabled
) {}
