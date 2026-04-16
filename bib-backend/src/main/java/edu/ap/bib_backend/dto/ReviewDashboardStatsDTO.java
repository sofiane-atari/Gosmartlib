package edu.ap.bib_backend.dto;

public record ReviewDashboardStatsDTO( 
    long totalFlaggedReviews, 
    long criticalFlaggedReviews ) {
}