package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolDeletionSchedulerTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolService schoolService;

    private SchoolDeletionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SchoolDeletionScheduler(schoolRepository, schoolService, 30L);
    }

    @Test
    void Given_NoExpiredSchools_When_ProcessScheduledDeletions_Then_DoesNothing() {
        when(schoolRepository.findByStatusAndMarkedForDeletionAtBefore(
                eq(SchoolStatus.MARKED_FOR_DELETION), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.processScheduledDeletions();

        verify(schoolService, never()).purge(anyLong());
    }

    @Test
    void Given_ExpiredSchools_When_ProcessScheduledDeletions_Then_PurgesEachSchool() {
        School school1 = new School();
        school1.setId(1L);
        school1.setName("School A");

        School school2 = new School();
        school2.setId(2L);
        school2.setName("School B");

        when(schoolRepository.findByStatusAndMarkedForDeletionAtBefore(
                eq(SchoolStatus.MARKED_FOR_DELETION), any(LocalDateTime.class)))
                .thenReturn(List.of(school1, school2));

        scheduler.processScheduledDeletions();

        verify(schoolService).purge(1L);
        verify(schoolService).purge(2L);
    }

    @Test
    void Given_ExpiredSchoolWithPurgeException_When_ProcessScheduledDeletions_Then_ContinuesWithNext() {
        School school1 = new School();
        school1.setId(1L);
        school1.setName("School A");

        School school2 = new School();
        school2.setId(2L);
        school2.setName("School B");

        when(schoolRepository.findByStatusAndMarkedForDeletionAtBefore(
                eq(SchoolStatus.MARKED_FOR_DELETION), any(LocalDateTime.class)))
                .thenReturn(List.of(school1, school2));

        doThrow(new RuntimeException("Purge failed")).when(schoolService).purge(1L);

        scheduler.processScheduledDeletions();

        verify(schoolService).purge(1L);
        verify(schoolService).purge(2L);
    }
}
