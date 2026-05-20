package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.LoanVolumePeriodDTO;
import edu.ap.bib_backend.dto.report.PopularLoanedBookDTO;
import edu.ap.bib_backend.dto.report.SchoolReportClassActivityDTO;
import edu.ap.bib_backend.dto.report.SchoolReportDTO;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.projection.report.ClassLoanActivityProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SchoolReportService {

    private final ReportAccessService reportAccessService;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    public SchoolReportService(
            ReportAccessService reportAccessService,
            UserRepository userRepository,
            LoanRepository loanRepository) {
        this.reportAccessService = reportAccessService;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
    }

    public SchoolReportDTO generateSchoolReport(Authentication authentication, int limit) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User niet gevonden"));

        Long schoolId = user.getSchool().getId();
        Long campusId = null;

        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<PopularLoanedBookDTO> popularBooks = loanRepository
                .findPopularLoanedBooksForSchool(schoolId, campusId, PageRequest.of(0, safeLimit))
                .stream()
                .map(book -> new PopularLoanedBookDTO(
                        book.getBookId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getImageUrl(),
                        book.getLoanCount() == null ? 0 : book.getLoanCount()))
                .toList();

        List<LoanVolumePeriodDTO> loanVolume = loanRepository.findMonthlyLoanVolumeForSchool(schoolId, campusId)
                .stream()
                .map(period -> new LoanVolumePeriodDTO(
                        String.format("%04d-%02d", period.getYear(), period.getMonth()),
                        period.getLoanCount() == null ? 0 : period.getLoanCount()))
                .toList();

        List<ClassLoanActivityProjection> activityRows = loanRepository.findClassLoanActivityForSchool(schoolId, campusId);
        List<SchoolReportClassActivityDTO> classActivity = activityRows.stream()
                .map(this::toClassActivity)
                .toList();

        return new SchoolReportDTO(
                schoolId,
                user.getSchool().getName(),
                null,
                null,
                List.of(),
                loanRepository.countLoanedBookCopiesForSchool(schoolId, campusId),
                loanRepository.countActiveLoanedBookCopiesForSchool(schoolId, campusId),
                (int) loanRepository.countClassesForSchool(schoolId, campusId),
                loanRepository.countStudentsWithClassForSchool(schoolId, campusId),
                popularBooks,
                loanVolume,
                classActivity);
    }

    private SchoolReportClassActivityDTO toClassActivity(ClassLoanActivityProjection row) {
        long studentCount = row.getStudentCount() == null ? 0 : row.getStudentCount();
        long totalLoans = row.getTotalLoans() == null ? 0 : row.getTotalLoans();
        long activeLoans = row.getActiveLoans() == null ? 0 : row.getActiveLoans();

        return new SchoolReportClassActivityDTO(
                row.getClassTitle(),
                studentCount,
                totalLoans,
                activeLoans,
                studentCount == 0 ? 0 : round((double) totalLoans / studentCount));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
