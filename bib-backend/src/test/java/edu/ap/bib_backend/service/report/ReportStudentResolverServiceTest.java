package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportStudentResolverServiceTest {

    private final OneRosterClient oneRosterClient = mock(OneRosterClient.class);

    private final ReportStudentResolverService service = new ReportStudentResolverService(oneRosterClient);

    @Test
    void resolveStudentsForClass_returnsDirectStudents_whenStudentsEndpointReturnsStudents() {
        OneRosterUserDTO student = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of(student));

        List<OneRosterUserDTO> result = service.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        assertThat(result).containsExactly(student);

        verify(oneRosterClient).fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        verify(oneRosterClient, never()).fetchEnrollmentsWithBearer(anyString(), anyString());
        verify(oneRosterClient, never()).fetchUsersWithBearer(anyString(), anyString());
    }

    @Test
    void resolveStudentsForClass_usesEnrollmentsFallback_whenDirectStudentsEndpointReturnsEmpty() {
        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of());

        OneRosterEnrollmentDTO enrollmentOne = new OneRosterEnrollmentDTO(
                "enrollment-1",
                "student",
                "student-1",
                "class-1");

        OneRosterEnrollmentDTO enrollmentTwo = new OneRosterEnrollmentDTO(
                "enrollment-2",
                "learner",
                "student-2",
                "class-1");

        OneRosterEnrollmentDTO teacherEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-3",
                "teacher",
                "teacher-1",
                "class-1");

        OneRosterEnrollmentDTO otherClassEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-4",
                "student",
                "student-3",
                "class-2");

        OneRosterEnrollmentDTO blankUserEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-5",
                "student",
                "   ",
                "class-1");

        OneRosterEnrollmentDTO nullRoleEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-6",
                null,
                "student-4",
                "class-1");

        when(oneRosterClient.fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(
                        enrollmentOne,
                        enrollmentTwo,
                        teacherEnrollment,
                        otherClassEnrollment,
                        blankUserEnrollment,
                        nullRoleEnrollment));

        OneRosterUserDTO studentTwo = new OneRosterUserDTO(
                "student-2",
                "Anna Janssens",
                "Anna",
                "Janssens",
                "anna.janssens",
                "student");

        OneRosterUserDTO studentOne = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        OneRosterUserDTO studentFour = new OneRosterUserDTO(
                "student-4",
                "Bram Willems",
                "Bram",
                "Willems",
                "bram.willems",
                "student");

        OneRosterUserDTO unrelatedUser = new OneRosterUserDTO(
                "student-3",
                "Clara Maes",
                "Clara",
                "Maes",
                "clara.maes",
                "student");

        when(oneRosterClient.fetchUsersWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(
                        unrelatedUser,
                        studentTwo,
                        studentOne,
                        studentFour));

        List<OneRosterUserDTO> result = service.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        assertThat(result)
                .extracting(OneRosterUserDTO::sourcedId)
                .containsExactly("student-2", "student-4", "student-1");

        assertThat(result)
                .extracting(OneRosterUserDTO::displayName)
                .containsExactly("Anna Janssens", "Bram Willems", "Jan Peeters");

        verify(oneRosterClient).fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        verify(oneRosterClient).fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token");

        verify(oneRosterClient).fetchUsersWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token");
    }

    @Test
    void resolveStudentsForClass_returnsEmptyList_whenFallbackHasNoMatchingStudentEnrollments() {
        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of());

        OneRosterEnrollmentDTO teacherEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-1",
                "teacher",
                "teacher-1",
                "class-1");

        OneRosterEnrollmentDTO otherClassEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-2",
                "student",
                "student-1",
                "class-2");

        when(oneRosterClient.fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(teacherEnrollment, otherClassEnrollment));

        List<OneRosterUserDTO> result = service.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        assertThat(result).isEmpty();

        verify(oneRosterClient).fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        verify(oneRosterClient).fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token");

        verify(oneRosterClient, never()).fetchUsersWithBearer(anyString(), anyString());
    }

    @Test
    void resolveStudentsForClass_ignoresEnrollmentUsersThatAreNotPresentInUsersResponse() {
        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of());

        OneRosterEnrollmentDTO existingUserEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-1",
                "student",
                "student-1",
                "class-1");

        OneRosterEnrollmentDTO missingUserEnrollment = new OneRosterEnrollmentDTO(
                "enrollment-2",
                "student",
                "student-missing",
                "class-1");

        when(oneRosterClient.fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(existingUserEnrollment, missingUserEnrollment));

        OneRosterUserDTO student = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        when(oneRosterClient.fetchUsersWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(student));

        List<OneRosterUserDTO> result = service.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        assertThat(result).containsExactly(student);
    }

    @Test
    void resolveStudentsForClass_handlesDuplicateUsersInUsersResponse() {
        when(oneRosterClient.fetchStudentsForClassWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1")).thenReturn(List.of());

        OneRosterEnrollmentDTO enrollment = new OneRosterEnrollmentDTO(
                "enrollment-1",
                "student",
                "student-1",
                "class-1");

        when(oneRosterClient.fetchEnrollmentsWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(enrollment));

        OneRosterUserDTO firstVersion = new OneRosterUserDTO(
                "student-1",
                "Jan Peeters",
                "Jan",
                "Peeters",
                "jan.peeters",
                "student");

        OneRosterUserDTO secondVersion = new OneRosterUserDTO(
                "student-1",
                "Jan Duplicate",
                "Jan",
                "Duplicate",
                "jan.duplicate",
                "student");

        when(oneRosterClient.fetchUsersWithBearer(
                "aphogeschool.smartschool.be",
                "smartschool-token")).thenReturn(List.of(firstVersion, secondVersion));

        List<OneRosterUserDTO> result = service.resolveStudentsForClass(
                "aphogeschool.smartschool.be",
                "smartschool-token",
                "class-1");

        assertThat(result).containsExactly(firstVersion);
    }
}