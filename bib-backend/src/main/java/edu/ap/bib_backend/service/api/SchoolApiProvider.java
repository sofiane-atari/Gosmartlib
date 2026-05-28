package edu.ap.bib_backend.service.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;

public interface SchoolApiProvider {

    SmartschoolProfileDto fetchProfile(String accessToken);

    SmartschoolGroupDto fetchGroups(String accessToken);

    SmartschoolGroupDto fetchGroupsForUser(String legacyUserId);

    List<Map<String, Object>> fetchUsers(String schoolCode);

    List<Map<String, Object>> fetchClasses(String schoolCode);

    List<Map<String, Object>> fetchEnrollments(String schoolCode);

    List<Map<String, Object>> fetchAcademicSessions(String schoolCode);

    List<Map<String, Object>> fetchOrgs(String schoolCode);

    Map<String, String> fetchUserDisplayNames(String schoolCode, Set<String> userIds);

    String findSourcedIdByLegacyIdentifier(String schoolCode, String legacyIdentifier);

    List<Map<String, Object>> fetchStudentsForClass(String schoolCode, String classSourcedId);
}
