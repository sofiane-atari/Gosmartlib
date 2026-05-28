package edu.ap.bib_backend.service.api;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.mock.MockSchoolData;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockSchoolApiProviderTest {

    @Mock
    private MockSchoolRegistry registry;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private MockSchoolApiProvider provider;

    private MockSchoolData goatheneum;

    @BeforeEach
    void setUp() {
        goatheneum = MockSchoolData.createTestSchool1();
    }

    @Test
    void Given_ValidToken_When_FetchProfile_Then_ReturnsProfileDto() {
        String token = "valid-token";
        when(jwtService.extractUsername(token)).thenReturn("T1s1LegacyId0000000000000000000000==");
        when(registry.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="))
                .thenReturn(goatheneum.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="));

        SmartschoolProfileDto profile = provider.fetchProfile(token);

        assertNotNull(profile);
        assertEquals("Jan", profile.firstName());
        assertEquals("Janssens", profile.lastName());
        assertEquals("jan.janssens@student.goatheneum.be", profile.email());
        assertEquals("T1s1LegacyId0000000000000000000000==", profile.userId());
    }

    @Test
    void Given_InvalidToken_When_FetchProfile_Then_ThrowsIllegalStateException() {
        when(jwtService.extractUsername("invalid-token")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> provider.fetchProfile("invalid-token"));
    }

    @Test
    void Given_ValidToken_When_FetchGroups_Then_ReturnsGroups() {
        String token = "valid-token";
        when(jwtService.extractUsername(token)).thenReturn("T1s1LegacyId0000000000000000000000==");
        when(registry.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="))
                .thenReturn(goatheneum.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="));

        SmartschoolGroupDto groups = provider.fetchGroups(token);

        assertNotNull(groups);
        assertEquals("3ITSOF1", groups.primaryGroupName());
        assertTrue(groups.parentGroupNames().containsAll(List.of("Leerlingen", "3de jaar")));
    }

    @Test
    void Given_InvalidToken_When_FetchGroups_Then_ThrowsIllegalStateException() {
        when(jwtService.extractUsername("bad-token")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> provider.fetchGroups("bad-token"));
    }

    @Test
    void Given_ExistingLegacyId_When_FetchGroupsForUser_Then_ReturnsGroups() {
        when(registry.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="))
                .thenReturn(goatheneum.getUserByLegacyId("T1s1LegacyId0000000000000000000000=="));

        SmartschoolGroupDto groups = provider.fetchGroupsForUser("T1s1LegacyId0000000000000000000000==");

        assertNotNull(groups);
        assertEquals("3ITSOF1", groups.primaryGroupName());
    }

    @Test
    void Given_NonExistingLegacyId_When_FetchGroupsForUser_Then_ReturnsEmptyGroups() {
        when(registry.getUserByLegacyId("nonexistent")).thenReturn(null);

        SmartschoolGroupDto groups = provider.fetchGroupsForUser("nonexistent");

        assertNotNull(groups);
        assertNull(groups.primaryGroupName());
        assertTrue(groups.parentGroupNames().isEmpty());
    }

    @Test
    void Given_ExistingSchoolCode_When_FetchUsers_Then_ReturnsUserRecords() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        List<Map<String, Object>> users = provider.fetchUsers("goatheneum");

        assertEquals(5, users.size());
        assertEquals("Jan", users.get(0).get("givenName"));
    }

    @Test
    void Given_ExistingSchoolDomain_When_FetchUsers_Then_ReturnsUserRecords() {
        when(registry.getSchoolByDomain("goatheneum.smartschool.be")).thenReturn(goatheneum);

        List<Map<String, Object>> users = provider.fetchUsers("goatheneum.smartschool.be");

        assertEquals(5, users.size());
    }

    @Test
    void Given_NonExistingSchool_When_FetchUsers_Then_ReturnsEmptyList() {
        List<Map<String, Object>> users = provider.fetchUsers("unknown");

        assertTrue(users.isEmpty());
    }

    @Test
    void Given_ExistingSchool_When_FetchClasses_Then_ReturnsClasses() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        List<Map<String, Object>> classes = provider.fetchClasses("goatheneum");

        assertEquals(2, classes.size());
        assertEquals("3ITSOF1", classes.get(0).get("title"));
    }

    @Test
    void Given_NonExistingSchool_When_FetchClasses_Then_ReturnsEmptyList() {
        List<Map<String, Object>> classes = provider.fetchClasses("unknown");

        assertTrue(classes.isEmpty());
    }

    @Test
    void Given_ExistingSchool_When_FetchEnrollments_Then_ReturnsEnrollments() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        List<Map<String, Object>> enrollments = provider.fetchEnrollments("goatheneum");

        assertEquals(5, enrollments.size());
    }

    @Test
    void Given_NonExistingSchool_When_FetchEnrollments_Then_ReturnsEmptyList() {
        List<Map<String, Object>> enrollments = provider.fetchEnrollments("unknown");

        assertTrue(enrollments.isEmpty());
    }

    @Test
    void Given_ExistingSchool_When_FetchAcademicSessions_Then_ReturnsSessions() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        List<Map<String, Object>> sessions = provider.fetchAcademicSessions("goatheneum");

        assertEquals(1, sessions.size());
        assertEquals("2025-2026", sessions.get(0).get("title"));
    }

    @Test
    void Given_NonExistingSchool_When_FetchAcademicSessions_Then_ReturnsEmptyList() {
        List<Map<String, Object>> sessions = provider.fetchAcademicSessions("unknown");

        assertTrue(sessions.isEmpty());
    }

    @Test
    void Given_ExistingSchool_When_FetchOrgs_Then_ReturnsOrgs() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        List<Map<String, Object>> orgs = provider.fetchOrgs("goatheneum");

        assertEquals(1, orgs.size());
    }

    @Test
    void Given_NonExistingSchool_When_FetchOrgs_Then_ReturnsEmptyList() {
        List<Map<String, Object>> orgs = provider.fetchOrgs("unknown");

        assertTrue(orgs.isEmpty());
    }

    @Test
    void Given_ExistingSchoolAndUserIds_When_FetchUserDisplayNames_Then_ReturnsMap() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        Set<String> userIds = Set.of("T1s1LegacyId0000000000000000000000==");
        Map<String, String> names = provider.fetchUserDisplayNames("goatheneum", userIds);

        assertEquals(1, names.size());
        assertTrue(names.containsValue("Jan Janssens"));
    }

    @Test
    void Given_NullUserIds_When_FetchUserDisplayNames_Then_ReturnsEmptyMap() {
        Map<String, String> names = provider.fetchUserDisplayNames("goatheneum", null);

        assertTrue(names.isEmpty());
    }

    @Test
    void Given_EmptyUserIds_When_FetchUserDisplayNames_Then_ReturnsEmptyMap() {
        Map<String, String> names = provider.fetchUserDisplayNames("goatheneum", Set.of());

        assertTrue(names.isEmpty());
    }

    @Test
    void Given_NonExistingSchool_When_FetchUserDisplayNames_Then_ReturnsEmptyMap() {
        Map<String, String> names = provider.fetchUserDisplayNames("unknown", Set.of("user-1"));

        assertTrue(names.isEmpty());
    }

    @Test
    void Given_ExistingLegacyId_When_FindSourcedIdByLegacyIdentifier_Then_ReturnsSourcedId() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        String sourcedId = provider.findSourcedIdByLegacyIdentifier(
                "goatheneum", "T1s1LegacyId0000000000000000000000==");

        assertNotNull(sourcedId);
        assertTrue(sourcedId.contains("8001"));
    }

    @Test
    void Given_NonExistingLegacyId_When_FindSourcedIdByLegacyIdentifier_Then_ReturnsNull() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        String sourcedId = provider.findSourcedIdByLegacyIdentifier("goatheneum", "nonexistent");

        assertNull(sourcedId);
    }

    @Test
    void Given_ExistingSchoolAndClass_When_FetchStudentsForClass_Then_ReturnsStudentRecords() {
        when(registry.getSchoolByCode("goatheneum")).thenReturn(goatheneum);

        String classSourcedId = goatheneum.getClasses().get(0).get("sourcedId").toString();
        List<Map<String, Object>> students = provider.fetchStudentsForClass("goatheneum", classSourcedId);

        assertEquals(2, students.size());
        assertEquals("student", students.get(0).get("role"));
    }

    @Test
    void Given_NonExistingSchool_When_FetchStudentsForClass_Then_ReturnsEmptyList() {
        List<Map<String, Object>> students = provider.fetchStudentsForClass("unknown", "class-1");

        assertTrue(students.isEmpty());
    }

    @Test
    void Given_JwtExtractionException_When_FetchProfile_Then_ThrowsIllegalStateException() {
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("JWT error"));

        assertThrows(IllegalStateException.class, () -> provider.fetchProfile("bad-token"));
    }
}
