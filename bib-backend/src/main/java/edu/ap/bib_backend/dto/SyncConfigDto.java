package edu.ap.bib_backend.dto;

public record SyncConfigDto(
    String syncTime,
    String syncZone,
    int syncIntervalDays,
    String syncCron
) {}
