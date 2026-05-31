package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportClassOptionDTO;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReportService {

    private final ReportAccessService reportAccessService;
    private final ReportContextService reportContextService;
    private final ReportClassMatcherService reportClassMatcherService;
    private final ReportStudentResolverService reportStudentResolverService;
    private final ReportBuilderService reportBuilderService;
    private final OneRosterClient oneRosterClient;
    private final ActiveSchoolContext activeSchoolContext;

    public ReportService(
            ReportAccessService reportAccessService,
            ReportContextService reportContextService,
            ReportClassMatcherService reportClassMatcherService,
            ReportStudentResolverService reportStudentResolverService,
            ReportBuilderService reportBuilderService,
            OneRosterClient oneRosterClient,
            ActiveSchoolContext activeSchoolContext) {
        this.reportAccessService = reportAccessService;
        this.reportContextService = reportContextService;
        this.reportClassMatcherService = reportClassMatcherService;
        this.reportStudentResolverService = reportStudentResolverService;
        this.reportBuilderService = reportBuilderService;
        this.oneRosterClient = oneRosterClient;
        this.activeSchoolContext = activeSchoolContext;
    }

    public List<ReportClassOptionDTO> getAvailableClasses(Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        ReportContextService.ReportContext context = reportContextService.getContext(authentication);

        List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClasses(context.schoolCode());

        List<OneRosterClassDTO> resultClasses;
        if (activeSchoolContext.isClassOverrideActive()) {
            List<String> overrideClassCodes = activeSchoolContext.getActiveClassCodes();
            List<String> overrideClassNames = activeSchoolContext.getActiveClassNames();
            resultClasses = oneRosterClasses.stream()
                    .filter(c -> overrideClassCodes.stream().anyMatch(code -> code.equalsIgnoreCase(c.classCode()))
                            || overrideClassNames.stream().anyMatch(name -> name.equalsIgnoreCase(c.title())))
                    .toList();
        } else {
            List<String> smartschoolGroupNames = reportContextService.getSmartschoolGroupNames(
                    context.accessToken(),
                    context.platform());

            resultClasses = reportClassMatcherService.findMatches(
                    smartschoolGroupNames,
                    oneRosterClasses);
        }

        return resultClasses.stream()
                .map(oneRosterClass -> new ReportClassOptionDTO(
                        oneRosterClass.sourcedId(),
                        oneRosterClass.title(),
                        oneRosterClass.classCode()))
                .toList();
    }

    public ClassReportDTO getClassReport(
            String classSourcedId,
            Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        ReportContextService.ReportContext context = reportContextService.getContext(authentication);

        List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClasses(context.schoolCode());

        OneRosterClassDTO selectedClass = oneRosterClasses.stream()
                .filter(oneRosterClass -> classSourcedId.equals(oneRosterClass.sourcedId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Klas niet gevonden"));

        if (!activeSchoolContext.isClassOverrideActive()) {
            List<String> smartschoolGroupNames = reportContextService.getSmartschoolGroupNames(
                    context.accessToken(),
                    context.platform());

            boolean hasAccessToClass = reportClassMatcherService
                    .findMatches(smartschoolGroupNames, oneRosterClasses)
                    .stream()
                    .anyMatch(oneRosterClass -> classSourcedId.equals(oneRosterClass.sourcedId()));

            if (!hasAccessToClass) {
                throw new ResponseStatusException(
                        FORBIDDEN,
                        "Je hebt geen toegang tot deze klas");
            }
        } else {
            List<String> overrideClassCodes = activeSchoolContext.getActiveClassCodes();
            List<String> overrideClassNames = activeSchoolContext.getActiveClassNames();
            boolean isOverridden = oneRosterClasses.stream()
                    .filter(c -> classSourcedId.equals(c.sourcedId()))
                    .anyMatch(c -> overrideClassCodes.stream().anyMatch(code -> code.equalsIgnoreCase(c.classCode()))
                            || overrideClassNames.stream().anyMatch(name -> name.equalsIgnoreCase(c.title())));
            if (!isOverridden) {
                throw new ResponseStatusException(
                        FORBIDDEN,
                        "Je hebt geen toegang tot deze klas");
            }
        }

        List<OneRosterUserDTO> students = reportStudentResolverService.resolveStudentsForClass(
                context.schoolCode(),
                context.accessToken(),
                selectedClass.sourcedId());

        return reportBuilderService.buildReport(selectedClass, students);
    }
}
