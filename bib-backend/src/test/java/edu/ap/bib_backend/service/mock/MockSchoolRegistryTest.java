package edu.ap.bib_backend.service.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockSchoolRegistryTest {

    private MockSchoolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MockSchoolRegistry();
    }

    @Test
    void Given_RegistryInitialized_When_GetAllSchools_Then_ReturnsTwoSchools() {
        List<MockSchoolData> schools = registry.getAllSchools();

        assertEquals(2, schools.size());
    }

    @Test
    void Given_InitialState_When_GetActiveSchool_Then_ReturnsGoatheneum() {
        MockSchoolData active = registry.getActiveSchool();

        assertNotNull(active);
        assertEquals("goatheneum", active.getSchoolCode());
    }

    @Test
    void Given_KuleuvenCode_When_SetActiveSchool_Then_ActiveSchoolChanges() {
        registry.setActiveSchool("kuleuven");

        assertEquals("kuleuven", registry.getActiveSchool().getSchoolCode());
    }

    @Test
    void Given_InvalidCode_When_SetActiveSchool_Then_ActiveSchoolUnchanged() {
        registry.setActiveSchool("non-existent");

        assertEquals("goatheneum", registry.getActiveSchool().getSchoolCode());
    }

    @Test
    void Given_GoatheneumClientId_When_GetSchoolByOnRosterClientId_Then_ReturnsSchool() {
        MockSchoolData school = registry.getSchoolByOnRosterClientId("ts1-client");

        assertNotNull(school);
        assertEquals("goatheneum", school.getSchoolCode());
    }

    @Test
    void Given_UnknownClientId_When_GetSchoolByOnRosterClientId_Then_ReturnsNull() {
        assertNull(registry.getSchoolByOnRosterClientId("unknown-client"));
    }

    @Test
    void Given_GoatheneumDomain_When_GetSchoolByDomain_Then_ReturnsSchool() {
        MockSchoolData school = registry.getSchoolByDomain("goatheneum.smartschool.be");

        assertNotNull(school);
        assertEquals("goatheneum", school.getSchoolCode());
    }

    @Test
    void Given_UnknownDomain_When_GetSchoolByDomain_Then_ReturnsNull() {
        assertNull(registry.getSchoolByDomain("unknown.smartschool.be"));
    }

    @Test
    void Given_KuleuvenCode_When_GetSchoolByCode_Then_ReturnsSchool() {
        MockSchoolData school = registry.getSchoolByCode("kuleuven");

        assertNotNull(school);
        assertEquals("kuleuven", school.getSchoolCode());
    }

    @Test
    void Given_UnknownCode_When_GetSchoolByCode_Then_ReturnsNull() {
        assertNull(registry.getSchoolByCode("non-existent"));
    }

    @Test
    void Given_ExistingLegacyId_When_GetUserByLegacyId_Then_ReturnsUser() {
        MockSchoolData.MockUser user = registry.getUserByLegacyId("T1s1LegacyId0000000000000000000000==");

        assertNotNull(user);
        assertEquals("Jan", user.givenName);
        assertEquals("Janssens", user.familyName);
    }

    @Test
    void Given_UnknownLegacyId_When_GetUserByLegacyId_Then_ReturnsNull() {
        assertNull(registry.getUserByLegacyId("nonexistent"));
    }

    @Test
    void Given_ExistingLegacyId_When_GetSchoolByLegacyId_Then_ReturnsSchool() {
        MockSchoolData school = registry.getSchoolByLegacyId("T1s1LegacyId0000000000000000000000==");

        assertNotNull(school);
        assertEquals("goatheneum", school.getSchoolCode());
    }

    @Test
    void Given_UnknownLegacyId_When_GetSchoolByLegacyId_Then_ReturnsNull() {
        assertNull(registry.getSchoolByLegacyId("nonexistent"));
    }

    @Test
    void Given_UserAndSchool_When_GenerateAuthCode_Then_ReturnsNonNullCode() {
        MockSchoolData school = registry.getSchoolByCode("goatheneum");
        MockSchoolData.MockUser user = school.getUserByUsername("sof3.jan.janssens");

        String code = registry.generateAuthCode(user, school);

        assertNotNull(code);
        assertTrue(code.startsWith("mock-code-"));
    }

    @Test
    void Given_GeneratedAuthCode_When_ResolveUserFromAuthCode_Then_ReturnsUser() {
        MockSchoolData school = registry.getSchoolByCode("goatheneum");
        MockSchoolData.MockUser user = school.getUserByUsername("sof3.jan.janssens");
        String code = registry.generateAuthCode(user, school);

        MockSchoolData.MockUser resolved = registry.resolveUserFromAuthCode(code);

        assertEquals(user, resolved);
    }

    @Test
    void Given_GeneratedAuthCode_When_GetSchoolByAuthCode_Then_ReturnsSchool() {
        MockSchoolData school = registry.getSchoolByCode("goatheneum");
        MockSchoolData.MockUser user = school.getUserByUsername("sof3.jan.janssens");
        String code = registry.generateAuthCode(user, school);

        MockSchoolData resolvedSchool = registry.getSchoolByAuthCode(code);

        assertEquals(school, resolvedSchool);
    }

    @Test
    void Given_InvalidAuthCode_When_ResolveUserFromAuthCode_Then_ReturnsNull() {
        assertNull(registry.resolveUserFromAuthCode("invalid-code"));
    }

    @Test
    void Given_CorrectCredentials_When_AuthenticateUserAnySchool_Then_ReturnsAuthResult() {
        MockSchoolRegistry.AuthResult result = registry.authenticateUserAnySchool(
                "sof3.jan.janssens", "janjanssens");

        assertNotNull(result);
        assertEquals("Jan", result.user().givenName);
        assertEquals("goatheneum", result.school().getSchoolCode());
    }

    @Test
    void Given_WrongPassword_When_AuthenticateUserAnySchool_Then_ReturnsNull() {
        MockSchoolRegistry.AuthResult result = registry.authenticateUserAnySchool(
                "sof3.jan.janssens", "wrongpassword");

        assertNull(result);
    }

    @Test
    void Given_UnknownUsername_When_AuthenticateUserAnySchool_Then_ReturnsNull() {
        MockSchoolRegistry.AuthResult result = registry.authenticateUserAnySchool(
                "unknown.user", "password");

        assertNull(result);
    }
}
