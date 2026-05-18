package edu.ap.bib_backend.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeacherStatsDTOTest {

    @Test
    void Given_Values_When_Instantiated_Then_FieldsAreSet() {
        // Arrange
        TeacherStatsDTO dto = new TeacherStatsDTO(10, 7, 3);

        // Act
        int totalTeachers = dto.totalTeachers();
        int activeTeachers = dto.activeTeachers();
        int librarians = dto.librarians();

        // Assert
        assertEquals(10, totalTeachers);
        assertEquals(7, activeTeachers);
        assertEquals(3, librarians);
    }
}
