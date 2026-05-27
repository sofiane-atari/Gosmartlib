package edu.ap.bib_backend.dto;

public record TeacherStatsDTO(
        int totalTeachers,
        int activeTeachers,
        int librarians,
        int admins) {
}
