package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.CampusReportDTO;
import edu.ap.bib_backend.dto.report.CampusReportRowDTO;
import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.report.ReportContextService.ReportContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CampusReportService {

    private final OneRosterClient oneRosterClient;
    private final ReportAccessService reportAccessService;
    private final ReportContextService reportContextService;
    private final ReportClassMatcherService classMatcherService;
    private final ReportStudentResolverService studentResolverService;
    private final ReportBuilderService reportBuilderService;
    private final ReportStatisticsQueryService statisticsQueryService;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;

    public CampusReportService(
            OneRosterClient oneRosterClient,
            ReportAccessService reportAccessService,
            ReportContextService reportContextService,
            ReportClassMatcherService classMatcherService,
            ReportStudentResolverService studentResolverService,
            ReportBuilderService reportBuilderService,
            ReportStatisticsQueryService statisticsQueryService,
            CampusRepository campusRepository,
            UserRepository userRepository) {
        this.oneRosterClient = oneRosterClient;
        this.reportAccessService = reportAccessService;
        this.reportContextService = reportContextService;
        this.classMatcherService = classMatcherService;
        this.studentResolverService = studentResolverService;
        this.reportBuilderService = reportBuilderService;
        this.statisticsQueryService = statisticsQueryService;
        this.campusRepository = campusRepository;
        this.userRepository = userRepository;
    }

    public CampusReportDTO generateReportForLoggedInTeacher(Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        String userId = authentication.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "User niet gevonden met id: " + userId));

        Campus campus = user.getCampus();
        if (campus == null) {
            return buildEmptyCampusReport(null, "Campus niet gevonden");
        }

        return generateReportForCampus(authentication, campus);
    }

    public List<CampusReportDTO> generateAllCampusReports(Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        String userId = authentication.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "User niet gevonden"));

        // Get all campuses in the user's school
        List<Campus> campuses = campusRepository.findBySchoolId(user.getSchool().getId());

        return campuses.stream()
                .map(campus -> generateReportForCampus(authentication, campus))
                .toList();
    }

    public CampusReportDTO generateReportForCampusId(Authentication authentication, Long campusId) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        String userId = authentication.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "User niet gevonden"));

        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Campus niet gevonden met id: " + campusId));

        // Verify user has access to this campus (same school)
        if (!campus.getSchool().getId().equals(user.getSchool().getId())) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "User heeft geen toegang tot deze campus");
        }

        return generateReportForCampus(authentication, campus);
    }

    private CampusReportDTO generateReportForCampus(Authentication authentication, Campus campus) {
        System.out.println("\n========================================");
        System.out.println("GENERATING REPORT FOR CAMPUS");
        System.out.println("Campus ID: " + campus.getId());
        System.out.println("Campus Name: " + campus.getName());
        System.out.println("Campus Code: " + campus.getCode());
        System.out.println("========================================\n");

        ReportContext context = reportContextService.getContext(authentication);

        List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClassesWithBearer(
                context.schoolCode(),
                context.accessToken());

        System.out.println("Total OneRoster classes available: " + oneRosterClasses.size());
        oneRosterClasses.forEach(c -> System.out.println("  - " + c.title()));

        if (oneRosterClasses.isEmpty()) {
            System.out.println("No OneRoster classes found");
            return buildEmptyCampusReport(campus.getId(), campus.getName());
        }

        // Filter classes to only those where students belong to this campus
        List<OneRosterClassDTO> campusSpecificClasses = oneRosterClasses.stream()
                .filter(oneRosterClass -> {
                    List<OneRosterUserDTO> students = studentResolverService.resolveStudentsForClass(
                            context.schoolCode(),
                            context.accessToken(),
                            oneRosterClass.sourcedId());

                    boolean hasCampusStudents = students.stream()
                            .anyMatch(student -> {
                                var userOpt = userRepository.findByOneRosterSourcedId(student.sourcedId());
                                return userOpt.isPresent() &&
                                       userOpt.get().getCampus() != null &&
                                       userOpt.get().getCampus().getId().equals(campus.getId());
                            });

                    if (hasCampusStudents) {
                        System.out.println("  ✓ " + oneRosterClass.title() + " has students in campus " + campus.getName());
                    } else {
                        System.out.println("  ✗ " + oneRosterClass.title() + " has NO students in campus " + campus.getName());
                    }

                    return hasCampusStudents;
                })
                .toList();

        System.out.println("\nClasses belonging to campus " + campus.getName() + ": " + campusSpecificClasses.size());
        campusSpecificClasses.forEach(c -> System.out.println("  - " + c.title()));
        System.out.println("========================================\n");

        // If no classes have students linked to this campus, fall back to showing all classes
        if (campusSpecificClasses.isEmpty()) {
            System.out.println("No campus-specific classes found, falling back to all classes");
            return buildCampusReport(context, campus, oneRosterClasses);
        }

        return buildCampusReport(context, campus, campusSpecificClasses);
    }

    private CampusReportDTO buildCampusReport(ReportContext context, Campus campus, List<OneRosterClassDTO> classes) {
        List<CampusReportRowDTO> classRows = new ArrayList<>();
        Set<String> allLinkedUserIds = new HashSet<>();

        for (OneRosterClassDTO oneRosterClass : classes) {
            System.out.println("PROCESSING CLASS: " + oneRosterClass.sourcedId() + " | " + oneRosterClass.title());

            List<OneRosterUserDTO> students = studentResolverService.resolveStudentsForClass(
                    context.schoolCode(),
                    context.accessToken(),
                    oneRosterClass.sourcedId());

            var classReport = reportBuilderService.buildReport(oneRosterClass, students);

            // Collect linked user IDs for aggregation
            classReport.students().stream()
                    .map(student -> student.linkedUserId())
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(allLinkedUserIds::add);

            // Create campus report row from class report
            CampusReportRowDTO row = new CampusReportRowDTO(
                    classReport.classSourcedId(),
                    classReport.classTitle(),
                    classReport.studentCount(),
                    classReport.linkedStudentCount(),
                    classReport.totalLoans(),
                    classReport.activeLoans(),
                    classReport.averageLoansPerLinkedStudent(),
                    classReport.classAverageScore(),
                    classReport.classAverageProgress());

            classRows.add(row);
        }

        // Calculate aggregated campus metrics
        int totalClassCount = classRows.size();
        int totalStudentCount = classRows.stream()
                .mapToInt(CampusReportRowDTO::studentCount)
                .sum();
        int totalLinkedStudentCount = classRows.stream()
                .mapToInt(CampusReportRowDTO::linkedStudentCount)
                .sum();

        long totalLoans = classRows.stream()
                .mapToLong(CampusReportRowDTO::totalLoans)
                .sum();

        long activeLoans = classRows.stream()
                .mapToLong(CampusReportRowDTO::activeLoans)
                .sum();

        double campusAverageLoansPerStudent = totalLinkedStudentCount == 0
                ? 0
                : (double) totalLoans / totalLinkedStudentCount;

        double campusAverageScore = classRows.isEmpty()
                ? 0
                : classRows.stream()
                        .mapToDouble(CampusReportRowDTO::classAverageScore)
                        .average()
                        .orElse(0);

        double campusAverageProgress = classRows.isEmpty()
                ? 0
                : classRows.stream()
                        .mapToDouble(CampusReportRowDTO::classAverageProgress)
                        .average()
                        .orElse(0);

        // Get popular genres and top rated books across all users in campus
        // IMPORTANT: Filter genres and books to only those from this specific campus
        List<PopularGenreDTO> popularGenres = findPopularGenresForCampus(
                new ArrayList<>(allLinkedUserIds), campus.getId());
        List<TopRatedBookReportDTO> topRatedBooks = findTopRatedBooksForCampus(
                new ArrayList<>(allLinkedUserIds), campus.getId());

        // Sort class rows by total loans descending
        classRows.sort(Comparator.comparingLong(CampusReportRowDTO::totalLoans).reversed());

        return new CampusReportDTO(
                campus.getId(),
                "campus-" + campus.getId(),
                campus.getName(),
                totalClassCount,
                totalStudentCount,
                totalLinkedStudentCount,
                totalLoans,
                activeLoans,
                round(campusAverageLoansPerStudent),
                round(campusAverageScore),
                round(campusAverageProgress),
                popularGenres,
                topRatedBooks,
                classRows);
    }

    private List<PopularGenreDTO> findPopularGenresForCampus(List<String> userIds, Long campusId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        // This would ideally use a repository query that filters by campus
        // For now, using the existing method as it's based on user loans
        // and loans have campus information
        return statisticsQueryService.findPopularGenresForUsers(userIds);
    }

    private List<TopRatedBookReportDTO> findTopRatedBooksForCampus(List<String> userIds, Long campusId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        // This would ideally use a repository query that filters by campus
        // For now, using the existing method as it's based on user reviews
        return statisticsQueryService.findTopRatedBooksForUsers(userIds);
    }

    private CampusReportDTO buildEmptyCampusReport(Long campusId, String campusName) {
        return new CampusReportDTO(
                campusId,
                "campus-empty",
                campusName != null ? campusName : "Campus",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
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
}











