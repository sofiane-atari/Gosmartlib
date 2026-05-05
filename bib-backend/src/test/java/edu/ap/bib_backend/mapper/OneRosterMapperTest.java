package edu.ap.bib_backend.mapper;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OneRosterMapperTest {

    private final OneRosterMapper mapper = new OneRosterMapper();

    @Test
    void toOneRosterClass_mapsFields() {
        Map<String, Object> source = Map.of(
                "sourcedId", "class-1",
                "title", "2ITSOF1",
                "classCode", "2ITSOF1");

        OneRosterClassDTO result = mapper.toOneRosterClass(source);

        assertThat(result.sourcedId()).isEqualTo("class-1");
        assertThat(result.title()).isEqualTo("2ITSOF1");
        assertThat(result.classCode()).isEqualTo("2ITSOF1");
    }

    @Test
    void toOneRosterClass_usesFallbackTitle_whenTitleMissing() {
        Map<String, Object> source = Map.of(
                "sourcedId", "class-1");

        OneRosterClassDTO result = mapper.toOneRosterClass(source);

        assertThat(result.sourcedId()).isEqualTo("class-1");
        assertThat(result.title()).isEqualTo("class-1");
    }

    @Test
    void toOneRosterUser_mapsFullNameFromGivenAndFamilyName() {
        Map<String, Object> source = Map.of(
                "sourcedId", "student-1",
                "givenName", "Jan",
                "familyName", "Peeters",
                "username", "jan.peeters",
                "role", "student");

        OneRosterUserDTO result = mapper.toOneRosterUser(source);

        assertThat(result.sourcedId()).isEqualTo("student-1");
        assertThat(result.displayName()).isEqualTo("Jan Peeters");
        assertThat(result.givenName()).isEqualTo("Jan");
        assertThat(result.familyName()).isEqualTo("Peeters");
        assertThat(result.username()).isEqualTo("jan.peeters");
        assertThat(result.role()).isEqualTo("student");
    }

    @Test
    void toOneRosterUser_usesUsernameWhenNameMissing() {
        Map<String, Object> source = Map.of(
                "sourcedId", "student-1",
                "username", "jan.peeters");

        OneRosterUserDTO result = mapper.toOneRosterUser(source);

        assertThat(result.displayName()).isEqualTo("jan.peeters");
    }

    @Test
    void toOneRosterEnrollment_mapsNestedUserAndClassObjects() {
        Map<String, Object> source = Map.of(
                "sourcedId", "enrollment-1",
                "role", "student",
                "user", Map.of("sourcedId", "student-1"),
                "class", Map.of("sourcedId", "class-1"));

        OneRosterEnrollmentDTO result = mapper.toOneRosterEnrollment(source);

        assertThat(result.sourcedId()).isEqualTo("enrollment-1");
        assertThat(result.role()).isEqualTo("student");
        assertThat(result.userSourcedId()).isEqualTo("student-1");
        assertThat(result.classSourcedId()).isEqualTo("class-1");
    }
}