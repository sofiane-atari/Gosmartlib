package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.LoanVolumePeriodDTO;
import edu.ap.bib_backend.dto.report.PopularLoanedBookDTO;
import edu.ap.bib_backend.dto.report.SchoolReportClassActivityDTO;
import edu.ap.bib_backend.dto.report.SchoolReportDTO;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.projection.report.ClassLoanActivityProjection;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SchoolReportService {

    private final ReportAccessService reportAccessService;
    private final SchoolRepository schoolRepository;
    private final LoanRepository loanRepository;
    private final ReportStatisticsQueryService statisticsQueryService;
    private final CurrentUserService currentUserService;

    public SchoolReportService(
            ReportAccessService reportAccessService,
            SchoolRepository schoolRepository,
            LoanRepository loanRepository,
            ReportStatisticsQueryService statisticsQueryService,
            CurrentUserService currentUserService) {
        this.reportAccessService = reportAccessService;
        this.schoolRepository = schoolRepository;
        this.loanRepository = loanRepository;
        this.statisticsQueryService = statisticsQueryService;
        this.currentUserService = currentUserService;
    }

    public SchoolReportDTO generateSchoolReport(Authentication authentication, Long requestedSchoolId, int limit, int offset) {
        reportAccessService.requireAuthenticated(authentication);
        reportAccessService.requireReportAccess(authentication);

        School activeSchool = currentUserService.getCurrentUserSchool(authentication);
        Long userSchoolId = activeSchool.getId();
        Long schoolId = requestedSchoolId == null ? userSchoolId : requestedSchoolId;
        reportAccessService.requireSameSchoolOrAdmin(authentication, userSchoolId, schoolId);

        School school = requestedSchoolId == null || requestedSchoolId.equals(userSchoolId)
                ? activeSchool
                : schoolRepository.findById(schoolId)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "School niet gevonden"));

        Long campusId = null;

        int safeLimit = Math.max(1, Math.min(limit, 20));
        int safeOffset = Math.max(0, offset);

        List<PopularLoanedBookDTO> popularBooks = loanRepository
                .findPopularLoanedBooksForSchool(schoolId, campusId, pageFromOffset(safeOffset, safeLimit))
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

        var popularGenres = statisticsQueryService.findPopularGenresForSchool(
                schoolId,
                campusId,
                PageRequest.of(0, 5));

        var topRatedBooks = statisticsQueryService.findTopRatedBooksForSchool(
                schoolId,
                campusId,
                PageRequest.of(0, 10));

        long totalLoans = loanRepository.countLoanedBookCopiesForSchool(schoolId, campusId);
        long activeLoans = loanRepository.countActiveLoanedBookCopiesForSchool(schoolId, campusId);
        long totalStudentCount = loanRepository.countStudentsWithClassForSchool(schoolId);

        long onTimeReturns = loanRepository.countOnTimeReturnsForSchool(schoolId, campusId);
        long lateReturns = loanRepository.countLateReturnsForSchool(schoolId, campusId);
        long totalReturns = onTimeReturns + lateReturns;
        double onTimePct = totalReturns == 0 ? 0 : round((double) onTimeReturns / totalReturns * 100);
        double latePct = totalReturns == 0 ? 0 : round((double) lateReturns / totalReturns * 100);

        return new SchoolReportDTO(
                schoolId,
                school.getName(),
                null,
                null,
                List.of(),
                totalLoans,
                activeLoans,
                (int) loanRepository.countClassesForSchool(schoolId),
                totalStudentCount,
                totalStudentCount == 0 ? 0 : round((double) totalLoans / totalStudentCount),
                popularGenres,
                topRatedBooks,
                popularBooks,
                loanVolume,
                classActivity,
                onTimeReturns,
                lateReturns,
                onTimePct,
                latePct);
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

    private Pageable pageFromOffset(int offset, int limit) {
        if (offset % limit == 0) {
            return PageRequest.of(offset / limit, limit);
        }

        return new OffsetLimitPageable(offset, limit);
    }

    private record OffsetLimitPageable(int offset, int limit) implements Pageable {

        @Override
        public int getPageNumber() {
            return offset / limit;
        }

        @Override
        public int getPageSize() {
            return limit;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return Sort.unsorted();
        }

        @Override
        public Pageable next() {
            return new OffsetLimitPageable(offset + limit, limit);
        }

        @Override
        public Pageable previousOrFirst() {
            return hasPrevious() ? new OffsetLimitPageable(Math.max(offset - limit, 0), limit) : first();
        }

        @Override
        public Pageable first() {
            return new OffsetLimitPageable(0, limit);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return new OffsetLimitPageable(pageNumber * limit, limit);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }
    }
}
