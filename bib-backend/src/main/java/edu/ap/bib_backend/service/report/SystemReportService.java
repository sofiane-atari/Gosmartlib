package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.LoanVolumePeriodDTO;
import edu.ap.bib_backend.dto.report.PopularLoanedBookDTO;
import edu.ap.bib_backend.dto.report.SystemReportDTO;
import edu.ap.bib_backend.dto.report.SystemSchoolActivityDTO;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.projection.report.SystemSchoolActivityProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SystemReportService {

    private final ReportAccessService reportAccessService;
    private final LoanRepository loanRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public SystemReportService(
            ReportAccessService reportAccessService,
            LoanRepository loanRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository) {
        this.reportAccessService = reportAccessService;
        this.loanRepository = loanRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    public SystemReportDTO generateSystemReport(Authentication authentication) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireAdmin(authentication);

        long totalLoans = loanRepository.countLoanedBookCopiesForSystem();
        long activeLoans = loanRepository.countActiveLoanedBookCopiesForSystem();
        long totalStudents = userRepository.countStudentsForSystem();
        long linkedStudents = userRepository.countLinkedStudentsForSystem();

        long onTimeReturns = loanRepository.countOnTimeReturnsForSystem();
        long lateReturns = loanRepository.countLateReturnsForSystem();

        List<PopularLoanedBookDTO> popularBooks = loanRepository
                .findPopularLoanedBooksForSystem(PageRequest.of(0, 10))
                .stream()
                .map(book -> new PopularLoanedBookDTO(
                        book.getBookId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getImageUrl(),
                        book.getLoanCount() == null ? 0 : book.getLoanCount()))
                .toList();

        List<LoanVolumePeriodDTO> loanVolume = loanRepository.findMonthlyLoanVolumeForSystem()
                .stream()
                .map(period -> new LoanVolumePeriodDTO(
                        String.format("%04d-%02d", period.getYear(), period.getMonth()),
                        period.getLoanCount() == null ? 0 : period.getLoanCount()))
                .toList();

        List<SystemSchoolActivityDTO> schools = userRepository.findSystemSchoolActivity()
                .stream()
                .map(this::toSchoolActivity)
                .toList();

        long totalReturns = onTimeReturns + lateReturns;
        double onTimePct = totalReturns == 0 ? 0 : round((double) onTimeReturns / totalReturns * 100);
        double latePct = totalReturns == 0 ? 0 : round((double) lateReturns / totalReturns * 100);

        return new SystemReportDTO(
                totalLoans,
                activeLoans,
                schoolRepository.count(),
                userRepository.countActiveUsersForSystem(),
                bookRepository.countUniqueTitlesForSystem(),
                bookCopyRepository.count(),
                totalStudents == 0 ? 0 : round(((double) linkedStudents / totalStudents) * 100),
                loanVolume,
                popularBooks,
                schools,
                onTimeReturns,
                lateReturns,
                onTimePct,
                latePct);
    }

    private SystemSchoolActivityDTO toSchoolActivity(SystemSchoolActivityProjection row) {
        long totalStudents = row.getTotalStudents() == null ? 0 : row.getTotalStudents();
        long linkedStudents = row.getLinkedStudents() == null ? 0 : row.getLinkedStudents();
        long totalLoans = row.getTotalLoans() == null ? 0 : row.getTotalLoans();
        long activeLoans = row.getActiveLoans() == null ? 0 : row.getActiveLoans();

        String lastSyncDate = null;
        if (row.getLastSyncAt() != null) {
            lastSyncDate = row.getLastSyncAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        return new SystemSchoolActivityDTO(
                row.getSchoolId(),
                row.getSchoolName(),
                null,
                totalStudents,
                linkedStudents,
                totalLoans,
                activeLoans,
                totalStudents == 0 ? 0 : round((double) totalLoans / totalStudents),
                lastSyncDate,
                "ok",
                null);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
