package edu.ap.bib_backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnvAdminServiceTest {

    private EnvAdminService createService(String usernames, String domains) {
        return new EnvAdminService(usernames, domains);
    }

    @Test
    @DisplayName("given empty env vars when isEnvAdmin then returns false")
    void givenEmptyEnvVars_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("", "");

        assertFalse(service.isEnvAdmin("user1", "school.smartschool.be"));
    }

    @Test
    @DisplayName("given matching username and domain when isEnvAdmin then returns true")
    void givenMatchingUsernameAndDomain_whenIsEnvAdmin_thenReturnsTrue() {
        EnvAdminService service = createService("alice,bob", "schoolA.smartschool.be,schoolB.smartschool.be");

        assertTrue(service.isEnvAdmin("alice", "schoolA.smartschool.be"));
        assertTrue(service.isEnvAdmin("bob", "schoolB.smartschool.be"));
    }

    @Test
    @DisplayName("given matching username but wrong domain when isEnvAdmin then returns false")
    void givenMatchingUsernameButWrongDomain_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "schoolA.smartschool.be");

        assertFalse(service.isEnvAdmin("alice", "schoolB.smartschool.be"));
    }

    @Test
    @DisplayName("given matching domain but wrong username when isEnvAdmin then returns false")
    void givenMatchingDomainButWrongUsername_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "schoolA.smartschool.be");

        assertFalse(service.isEnvAdmin("bob", "schoolA.smartschool.be"));
    }

    @Test
    @DisplayName("given domain matching case-insensitively when isEnvAdmin then returns true")
    void givenDomainCaseInsensitive_whenIsEnvAdmin_thenReturnsTrue() {
        EnvAdminService service = createService("alice", "SchoolA.Smartschool.be");

        assertTrue(service.isEnvAdmin("alice", "schoola.smartschool.be"));
    }

    @Test
    @DisplayName("given username is case-sensitive when isEnvAdmin then returns false for different case")
    void givenUsernameCaseSensitive_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "schoolA.smartschool.be");

        assertFalse(service.isEnvAdmin("Alice", "schoolA.smartschool.be"));
    }

    @Test
    @DisplayName("given null userId when isEnvAdmin then returns false")
    void givenNullUserId_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "schoolA.smartschool.be");

        assertFalse(service.isEnvAdmin(null, "schoolA.smartschool.be"));
    }

    @Test
    @DisplayName("given null schoolDomain when isEnvAdmin then returns false")
    void givenNullSchoolDomain_whenIsEnvAdmin_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "schoolA.smartschool.be");

        assertFalse(service.isEnvAdmin("alice", null));
    }

    @Test
    @DisplayName("given matching userId when isEnvAdminByUserId then returns true")
    void givenMatchingUserId_whenIsEnvAdminByUserId_thenReturnsTrue() {
        EnvAdminService service = createService("alice,bob,charlie", "domA,domB,domC");

        assertTrue(service.isEnvAdminByUserId("alice"));
        assertTrue(service.isEnvAdminByUserId("charlie"));
    }

    @Test
    @DisplayName("given non-matching userId when isEnvAdminByUserId then returns false")
    void givenNonMatchingUserId_whenIsEnvAdminByUserId_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "domA");

        assertFalse(service.isEnvAdminByUserId("bob"));
    }

    @Test
    @DisplayName("given null userId when isEnvAdminByUserId then returns false")
    void givenNullUserId_whenIsEnvAdminByUserId_thenReturnsFalse() {
        EnvAdminService service = createService("alice", "domA");

        assertFalse(service.isEnvAdminByUserId(null));
    }

    @Test
    @DisplayName("given empty admin lists when isEnvAdminByUserId then returns false")
    void givenEmptyAdminLists_whenIsEnvAdminByUserId_thenReturnsFalse() {
        EnvAdminService service = createService("", "");

        assertFalse(service.isEnvAdminByUserId("anyone"));
    }

    @Test
    @DisplayName("given usernames with whitespace when isEnvAdmin then trims and matches")
    void givenUsernamesWithWhitespace_whenIsEnvAdmin_thenTrimsAndMatches() {
        EnvAdminService service = createService(" alice ,  bob  ", "domA,domB");

        assertTrue(service.isEnvAdmin("alice", "domA"));
        assertTrue(service.isEnvAdmin("bob", "domB"));
    }

    @Test
    @DisplayName("given domains with whitespace when isEnvAdmin then trims and matches")
    void givenDomainsWithWhitespace_whenIsEnvAdmin_thenTrimsAndMatches() {
        EnvAdminService service = createService("alice", " domA.smartschool.be , domB.smartschool.be ");

        assertTrue(service.isEnvAdmin("alice", "domA.smartschool.be"));
    }

    @Test
    @DisplayName("given admin usernames when getAdminUsernames then returns unmodifiable list")
    void givenAdminUsernames_whenGetAdminUsernames_thenReturnsUnmodifiableList() {
        EnvAdminService service = createService("alice,bob", "domA,domB");

        List<String> usernames = service.getAdminUsernames();

        assertTrue(usernames.contains("alice"));
        assertTrue(usernames.contains("bob"));
    }

    @ParameterizedTest
    @CsvSource({
            "alice,alice,domA,domA,true",
            "user1,user1,dom1,dom1,true",
            "user1,user2,dom1,dom1,false",
            "user1,user1,dom1,dom2,false",
    })
    @DisplayName("given indexed admin pair when isEnvAdmin then matches correctly")
    void givenIndexedAdminPair_whenIsEnvAdmin_thenMatchesCorrectly(
            String adminUser, String testUser,
            String adminDomain, String testDomain,
            boolean expected) {
        EnvAdminService service = createService(adminUser, adminDomain);

        if (expected) {
            assertTrue(service.isEnvAdmin(testUser, testDomain));
        } else {
            assertFalse(service.isEnvAdmin(testUser, testDomain));
        }
    }
}
