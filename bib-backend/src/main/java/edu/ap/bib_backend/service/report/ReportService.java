package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportClassOptionDTO;
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

    public ReportService(
            ReportAccessService reportAccessService,
            ReportContextService reportContextService,
            ReportClassMatcherService reportClassMatcherService,
            ReportStudentResolverService reportStudentResolverService,
            ReportBuilderService reportBuilderService,
            OneRosterClient oneRosterClient) {
        this.reportAccessService = reportAccessService;
        this.reportContextService = reportContextService;
        this.reportClassMatcherService = reportClassMatcherService;
        this.reportStudentResolverService = reportStudentResolverService;
        this.reportBuilderService = reportBuilderService;
        this.oneRosterClient = oneRosterClient;
    }

    public List<ReportClassOptionDTO> getAvailableClasses(Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        ReportContextService.ReportContext context = reportContextService.getContext(authentication);

        List<OneRosterClassDTO> oneRosterClasses = oneRosterClient.fetchClasses(context.schoolCode());

        List<String> smartschoolGroupNames = reportContextService.getSmartschoolGroupNames(
                context.accessToken(),
                context.platform());

        List<OneRosterClassDTO> matchedClasses = reportClassMatcherService.findMatches(
                smartschoolGroupNames,
                oneRosterClasses);

        return matchedClasses.stream()
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

        List<OneRosterUserDTO> students = reportStudentResolverService.resolveStudentsForClass(
                context.schoolCode(),
                context.accessToken(),
                selectedClass.sourcedId());

        return reportBuilderService.buildReport(selectedClass, students);
    }
}