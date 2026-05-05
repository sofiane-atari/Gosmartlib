package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.mapper.ReportBuilderService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.report.ReportContextService.ReportContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassReportService {

        private final OneRosterClient oneRosterClient;
        private final ReportAccessService reportAccessService;
        private final ReportContextService reportContextService;
        private final ReportClassMatcherService classMatcherService;
        private final ReportStudentResolverService studentResolverService;
        private final ReportBuilderService reportBuilderService;

        public ClassReportService(
                        OneRosterClient oneRosterClient,
                        ReportAccessService reportAccessService,
                        ReportContextService reportContextService,
                        ReportClassMatcherService classMatcherService,
                        ReportStudentResolverService studentResolverService,
                        ReportBuilderService reportBuilderService) {
                this.oneRosterClient = oneRosterClient;
                this.reportAccessService = reportAccessService;
                this.reportContextService = reportContextService;
                this.classMatcherService = classMatcherService;
                this.studentResolverService = studentResolverService;
                this.reportBuilderService = reportBuilderService;
        }

        public List<ClassReportDTO> generateReportsForLoggedInTeacher(Authentication authentication) {
                reportAccessService.requireAuthenticated(authentication);
                reportAccessService.requireReportAccess(authentication);

                ReportContext context = reportContextService.getContext(authentication);

                List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClassesWithBearer(
                                context.schoolCode(),
                                context.accessToken());

                logOneRosterClasses(oneRosterClasses);

                Optional<OneRosterClassDTO> classFromJwt = findClassFromJwt(
                                authentication,
                                oneRosterClasses);

                if (classFromJwt.isPresent()) {
                        System.out.println("MATCH SOURCE = JWT classCode");

                        return List.of(buildReport(
                                        context,
                                        classFromJwt.get()));
                }

                List<String> smartschoolGroupNames = reportContextService.getSmartschoolGroupNames(
                                context.accessToken(),
                                context.platform());

                logSmartschoolGroupNames(smartschoolGroupNames);

                if (smartschoolGroupNames.isEmpty()) {
                        return List.of();
                }

                List<OneRosterClassDTO> matchedClasses = classMatcherService.findMatches(
                                smartschoolGroupNames,
                                oneRosterClasses);

                return matchedClasses.stream()
                                .map(oneRosterClass -> buildReport(context, oneRosterClass))
                                .toList();
        }

        private Optional<OneRosterClassDTO> findClassFromJwt(
                        Authentication authentication,
                        List<OneRosterClassDTO> oneRosterClasses) {
                String classCodeFromJwt = reportContextService.extractClassCodeFromAuthentication(authentication);

                System.out.println("JWT classCode = " + classCodeFromJwt);

                return classMatcherService.findMatch(
                                classCodeFromJwt,
                                oneRosterClasses);
        }

        private ClassReportDTO buildReport(
                        ReportContext context,
                        OneRosterClassDTO oneRosterClass) {
                System.out.println("MATCHED CLASS sourcedId = " + oneRosterClass.sourcedId());
                System.out.println("MATCHED CLASS title = " + oneRosterClass.title());
                System.out.println("MATCHED CLASS classCode = " + oneRosterClass.classCode());

                List<OneRosterUserDTO> students = studentResolverService.resolveStudentsForClass(
                                context.schoolCode(),
                                context.accessToken(),
                                oneRosterClass.sourcedId());

                return reportBuilderService.buildReport(
                                oneRosterClass,
                                students);
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