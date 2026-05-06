package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportStudentResolverService {

    private final OneRosterClient oneRosterClient;

    public ReportStudentResolverService(OneRosterClient oneRosterClient) {
        this.oneRosterClient = oneRosterClient;
    }

    public List<OneRosterUserDTO> resolveStudentsForClass(
            String schoolCode,
            String smartschoolAccessToken,
            String classSourcedId) {
        List<OneRosterUserDTO> students = oneRosterClient.fetchStudentsForClassWithBearer(
                schoolCode,
                smartschoolAccessToken,
                classSourcedId);

        if (!students.isEmpty()) {
            return students;
        }

        System.out.println("Direct students endpoint gaf 0 studenten. Probeer fallback via /enrollments.");

        return fetchStudentsViaEnrollmentsFallback(
                schoolCode,
                smartschoolAccessToken,
                classSourcedId);
    }

    private List<OneRosterUserDTO> fetchStudentsViaEnrollmentsFallback(
            String schoolCode,
            String smartschoolAccessToken,
            String classSourcedId) {
        List<OneRosterEnrollmentDTO> enrollments = oneRosterClient.fetchEnrollmentsWithBearer(
                schoolCode,
                smartschoolAccessToken);

        System.out.println("Aantal enrollments ontvangen = " + enrollments.size());

        Set<String> studentUserIdsInClass = enrollments.stream()
                .filter(enrollment -> classSourcedId.equals(enrollment.classSourcedId()))
                .filter(enrollment -> enrollment.role() == null
                        || enrollment.role().equalsIgnoreCase("student")
                        || enrollment.role().equalsIgnoreCase("learner"))
                .map(OneRosterEnrollmentDTO::userSourcedId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        System.out.println("Aantal student user ids voor klas " + classSourcedId + " = "
                + studentUserIdsInClass.size());

        if (studentUserIdsInClass.isEmpty()) {
            return List.of();
        }

        List<OneRosterUserDTO> allUsers = oneRosterClient.fetchUsersWithBearer(
                schoolCode,
                smartschoolAccessToken);

        Map<String, OneRosterUserDTO> usersById = allUsers.stream()
                .collect(Collectors.toMap(
                        OneRosterUserDTO::sourcedId,
                        Function.identity(),
                        (left, right) -> left));

        return studentUserIdsInClass.stream()
                .map(usersById::get)
                .filter(user -> user != null)
                .sorted(Comparator.comparing(
                        OneRosterUserDTO::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}