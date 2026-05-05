package edu.ap.bib_backend.mapper;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OneRosterMapper {

    public OneRosterClassDTO toOneRosterClass(Map<String, Object> source) {
        String sourcedId = firstNonBlank(source, "sourcedId", "identifier");
        String title = firstNonBlank(source, "title", "name", "classCode", "identifier", "sourcedId");
        String classCode = firstNonBlank(source, "classCode", "code", "identifier");

        if (title == null) {
            title = "Onbekende klas";
        }

        return new OneRosterClassDTO(
                sourcedId,
                title,
                classCode);
    }

    public OneRosterUserDTO toOneRosterUser(Map<String, Object> source) {
        String sourcedId = firstNonBlank(source, "sourcedId", "identifier", "userId", "username");
        String givenName = firstNonBlank(source, "givenName", "firstName");
        String familyName = firstNonBlank(source, "familyName", "lastName", "surname");
        String username = firstNonBlank(source, "username", "userName");
        String role = firstNonBlank(source, "role", "roles");

        String displayName = buildDisplayName(source);

        return new OneRosterUserDTO(
                sourcedId,
                displayName,
                givenName,
                familyName,
                username,
                role);
    }

    public OneRosterEnrollmentDTO toOneRosterEnrollment(Map<String, Object> source) {
        String sourcedId = firstNonBlank(source, "sourcedId", "identifier");
        String role = firstNonBlank(source, "role");

        String userSourcedId = firstNonBlank(source, "userSourcedId", "userId", "user", "user_id");
        String classSourcedId = firstNonBlank(source, "classSourcedId", "classId", "class", "class_id");

        Object userObject = source.get("user");
        if ((userSourcedId == null || userSourcedId.isBlank()) && userObject instanceof Map<?, ?> userMap) {
            userSourcedId = firstNonBlankFromRawMap(userMap, "sourcedId", "identifier", "userId", "username");
        }

        Object classObject = source.get("class");
        if ((classSourcedId == null || classSourcedId.isBlank()) && classObject instanceof Map<?, ?> classMap) {
            classSourcedId = firstNonBlankFromRawMap(classMap, "sourcedId", "identifier", "classCode");
        }

        return new OneRosterEnrollmentDTO(
                sourcedId,
                role,
                userSourcedId,
                classSourcedId);
    }

    public String buildDisplayName(Map<String, Object> user) {
        String fullName = firstNonBlank(user, "fullName", "name", "displayName");

        if (fullName != null) {
            return fullName;
        }

        String givenName = firstNonBlank(user, "givenName", "firstName");
        String familyName = firstNonBlank(user, "familyName", "lastName", "surname");

        String combined = ((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName)).trim();

        if (!combined.isBlank()) {
            return combined;
        }

        String username = firstNonBlank(user, "username", "userName");

        return username != null ? username : "Onbekende gebruiker";
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);

            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }

        return null;
    }

    private String firstNonBlankFromRawMap(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);

            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }

        return null;
    }
}