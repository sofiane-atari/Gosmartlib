package edu.ap.bib_backend.service.statistic;

import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.GroupInfoItemDTO;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportRowDTO;
import edu.ap.bib_backend.jpa.entity.statistic.StudentClassStatistic;
import edu.ap.bib_backend.jpa.repository.StudentClassStatisticRepository;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ClassReportService {

        private final OneRosterClient oneRosterClient;
        private final TokenStorageService tokenStorageService;
        private final SmartschoolProfileService smartschoolProfileService;
        private final StudentClassStatisticRepository statisticRepository;

        public ClassReportService(
                        OneRosterClient oneRosterClient,
                        TokenStorageService tokenStorageService,
                        SmartschoolProfileService smartschoolProfileService,
                        StudentClassStatisticRepository statisticRepository) {
                this.oneRosterClient = oneRosterClient;
                this.tokenStorageService = tokenStorageService;
                this.smartschoolProfileService = smartschoolProfileService;
                this.statisticRepository = statisticRepository;
        }

        public List<ClassReportDTO> generateReportsForLoggedInTeacher(Authentication authentication) {
                requireAuthenticated(authentication);
                requireTeacherOrStudentForTesting(authentication);

                String userId = authentication.getName();
                String smartschoolAccessToken = tokenStorageService.getSmartschoolAccessToken(userId);

                if (smartschoolAccessToken == null || smartschoolAccessToken.isBlank()) {
                        throw new ResponseStatusException(
                                        UNAUTHORIZED,
                                        "Geen Smartschool access token gevonden");
                }

                SmartschoolProfileDto profile = smartschoolProfileService.fetchProfile(smartschoolAccessToken);

                if (profile == null || profile.platform() == null || profile.platform().isBlank()) {
                        throw new ResponseStatusException(
                                        UNAUTHORIZED,
                                        "Geen Smartschool platform gevonden");
                }

                String schoolCode = extractHostFromPlatform(profile.platform());

                List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClassesWithBearer(
                                schoolCode,
                                smartschoolAccessToken);

                logOneRosterClasses(oneRosterClasses);

                Optional<OneRosterClassDTO> classFromJwt = findClassFromJwtDetails(
                                authentication,
                                oneRosterClasses);

                if (classFromJwt.isPresent()) {
                        System.out.println("MATCH SOURCE = JWT classCode");
                        return List.of(buildReportForClass(
                                        schoolCode,
                                        smartschoolAccessToken,
                                        classFromJwt.get()));
                }

                SmartschoolGroupInfoDTO groupInfo = smartschoolProfileService.fetchGroupInfo(
                                smartschoolAccessToken,
                                profile.platform());

                List<String> smartschoolGroupNames = extractGroupNames(groupInfo);

                logSmartschoolGroupNames(smartschoolGroupNames);

                if (smartschoolGroupNames.isEmpty()) {
                        return List.of();
                }

                return smartschoolGroupNames.stream()
                                .map(groupName -> findMatchingOneRosterClass(groupName, oneRosterClasses))
                                .flatMap(Optional::stream)
                                .distinct()
                                .peek(oneRosterClass -> {
                                        System.out.println("MATCH SOURCE = Smartschool groupinfo");
                                        System.out.println("MATCHED CLASS sourcedId = " + oneRosterClass.sourcedId());
                                        System.out.println("MATCHED CLASS title = " + oneRosterClass.title());
                                        System.out.println("MATCHED CLASS classCode = " + oneRosterClass.classCode());
                                })
                                .map(oneRosterClass -> buildReportForClass(
                                                schoolCode,
                                                smartschoolAccessToken,
                                                oneRosterClass))
                                .toList();
        }

        private Optional<OneRosterClassDTO> findClassFromJwtDetails(
                        Authentication authentication,
                        List<OneRosterClassDTO> oneRosterClasses) {
                String classCodeFromJwt = extractClassCodeFromAuthentication(authentication);
                String schoolCodeFromJwt = extractSchoolCodeFromAuthentication(authentication);

                System.out.println("JWT classCode = " + classCodeFromJwt);
                System.out.println("JWT schoolCode = " + schoolCodeFromJwt);

                if (classCodeFromJwt == null || classCodeFromJwt.isBlank()) {
                        return Optional.empty();
                }

                Optional<OneRosterClassDTO> match = findMatchingOneRosterClass(
                                classCodeFromJwt,
                                oneRosterClasses);

                match.ifPresent(oneRosterClass -> {
                        System.out.println("MATCHED CLASS sourcedId = " + oneRosterClass.sourcedId());
                        System.out.println("MATCHED CLASS title = " + oneRosterClass.title());
                        System.out.println("MATCHED CLASS classCode = " + oneRosterClass.classCode());
                });

                return match;
        }

        private TeacherClassReportBuildResult buildRowsForClass(
                        String schoolCode,
                        String smartschoolAccessToken,
                        OneRosterClassDTO oneRosterClass) {
                List<OneRosterUserDTO> students = oneRosterClient
                                .fetchStudentsForClassWithBearer(
                                                schoolCode,
                                                smartschoolAccessToken,
                                                oneRosterClass.sourcedId());

                if (students.isEmpty()) {
                        System.out.println(
                                        "Direct students endpoint gaf 0 studenten. Probeer fallback via /enrollments.");
                        students = fetchStudentsViaEnrollmentsFallback(
                                        schoolCode,
                                        smartschoolAccessToken,
                                        oneRosterClass.sourcedId());
                }

                List<ReportRowDTO> rows = students.stream()
                                .map(student -> toReportRow(student, oneRosterClass.sourcedId()))
                                .toList();

                return new TeacherClassReportBuildResult(rows);
        }

        private ClassReportDTO buildReportForClass(
                        String schoolCode,
                        String smartschoolAccessToken,
                        OneRosterClassDTO oneRosterClass) {
                System.out.println("BUILD REPORT CLASS sourcedId = " + oneRosterClass.sourcedId());
                System.out.println("BUILD REPORT CLASS title = " + oneRosterClass.title());
                System.out.println("BUILD REPORT CLASS classCode = " + oneRosterClass.classCode());

                TeacherClassReportBuildResult result = buildRowsForClass(
                                schoolCode,
                                smartschoolAccessToken,
                                oneRosterClass);

                List<ReportRowDTO> rows = result.rows();

                double classAverageScore = rows.isEmpty()
                                ? 0
                                : rows.stream()
                                                .mapToDouble(ReportRowDTO::averageScore)
                                                .average()
                                                .orElse(0);

                double classAverageProgress = rows.isEmpty()
                                ? 0
                                : rows.stream()
                                                .mapToDouble(ReportRowDTO::progressPercentage)
                                                .average()
                                                .orElse(0);

                return new ClassReportDTO(
                                oneRosterClass.sourcedId(),
                                oneRosterClass.title(),
                                rows.size(),
                                round(classAverageScore),
                                round(classAverageProgress),
                                rows);
        }

        private ReportRowDTO toReportRow(
                        OneRosterUserDTO student,
                        String classSourcedId) {
                StudentClassStatistic statistic = statisticRepository
                                .findByStudentSourcedIdAndClassSourcedId(student.sourcedId(), classSourcedId)
                                .orElse(null);

                if (statistic == null) {
                        return new ReportRowDTO(
                                        student.sourcedId(),
                                        student.displayName(),
                                        0,
                                        0,
                                        0,
                                        0,
                                        0);
                }

                double progress = statistic.getTotalItems() == 0
                                ? 0
                                : ((double) statistic.getCompletedItems() / statistic.getTotalItems()) * 100;

                return new ReportRowDTO(
                                student.sourcedId(),
                                student.displayName(),
                                statistic.getTotalItems(),
                                statistic.getCompletedItems(),
                                round(progress),
                                round(statistic.getAverageScore()),
                                statistic.getTotalTimeMinutes());
        }

        private List<String> extractGroupNames(SmartschoolGroupInfoDTO groupInfo) {
                if (groupInfo == null || groupInfo.groups() == null) {
                        return List.of();
                }

                return groupInfo.groups()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(GroupInfoItemDTO::name)
                                .filter(name -> name != null && !name.isBlank())
                                .map(String::trim)
                                .filter(name -> !name.equalsIgnoreCase("Leerlingen"))
                                .filter(name -> !name.equalsIgnoreCase("Leerkrachten"))
                                .filter(name -> !name.equalsIgnoreCase("Personeel"))
                                .distinct()
                                .toList();
        }

        private Optional<OneRosterClassDTO> findMatchingOneRosterClass(
                        String classOrGroupName,
                        List<OneRosterClassDTO> oneRosterClasses) {
                String normalizedInput = normalize(classOrGroupName);

                return oneRosterClasses.stream()
                                .filter(oneRosterClass -> {
                                        String title = normalize(oneRosterClass.title());
                                        String classCode = normalize(oneRosterClass.classCode());
                                        String sourcedId = normalize(oneRosterClass.sourcedId());

                                        return classCode.equals(normalizedInput)
                                                        || title.equals(normalizedInput)
                                                        || sourcedId.equals(normalizedInput);
                                })
                                .findFirst();
        }

        @SuppressWarnings("unchecked")
        private String extractClassCodeFromAuthentication(Authentication authentication) {
                Object details = authentication.getDetails();

                if (details instanceof java.util.Map<?, ?> detailsMap) {
                        Object classCode = detailsMap.get("classCode");

                        if (classCode instanceof String text && !text.isBlank()) {
                                return text.trim();
                        }
                }

                return null;
        }

        @SuppressWarnings("unchecked")
        private String extractSchoolCodeFromAuthentication(Authentication authentication) {
                Object details = authentication.getDetails();

                if (details instanceof java.util.Map<?, ?> detailsMap) {
                        Object schoolCode = detailsMap.get("schoolCode");

                        if (schoolCode instanceof String text && !text.isBlank()) {
                                return text.trim();
                        }
                }

                return null;
        }

        private void requireAuthenticated(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new ResponseStatusException(
                                        UNAUTHORIZED,
                                        "Je moet ingelogd zijn om klasrapporten te bekijken");
                }
        }

        private void requireTeacherOrStudentForTesting(Authentication authentication) {
                authentication.getAuthorities()
                                .forEach(authority -> System.out
                                                .println("REPORT AUTHORITY = " + authority.getAuthority()));

                boolean allowed = authentication.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(role -> role.equalsIgnoreCase("ROLE_TEACHER")
                                                || role.equalsIgnoreCase("ROLE_STUDENT")
                                                || role.equalsIgnoreCase("ROLE_ADMIN"));

                if (!allowed) {
                        throw new ResponseStatusException(
                                        FORBIDDEN,
                                        "Alleen leerkrachten kunnen klasrapporten bekijken");
                }
        }

        private String extractHostFromPlatform(String platform) {
                try {
                        URI uri = URI.create(platform);

                        if (uri.getHost() == null || uri.getHost().isBlank()) {
                                throw new IllegalArgumentException();
                        }

                        return uri.getHost();
                } catch (Exception ex) {
                        throw new ResponseStatusException(
                                        UNAUTHORIZED,
                                        "Geen geldige Smartschool platform URL gevonden");
                }
        }

        private String normalize(String value) {
                if (value == null) {
                        return "";
                }

                return value
                                .trim()
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("\\s+", " ");
        }

        private void logSmartschoolGroupNames(List<String> smartschoolGroupNames) {
                System.out.println("=== SMARTSCHOOL GROUP NAMES ===");
                smartschoolGroupNames.forEach(System.out::println);
        }

        private void logOneRosterClasses(List<OneRosterClassDTO> oneRosterClasses) {
                System.out.println("=== ONEROSTER CLASSES ===");
                oneRosterClasses.forEach(c -> System.out.println(
                                "sourcedId=" + c.sourcedId()
                                                + " | title=" + c.title()
                                                + " | classCode=" + c.classCode()));
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        private record TeacherClassReportBuildResult(
                        List<ReportRowDTO> rows) {
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
                                .sorted(java.util.Comparator.comparing(OneRosterUserDTO::displayName,
                                                String.CASE_INSENSITIVE_ORDER))
                                .toList();
        }
}