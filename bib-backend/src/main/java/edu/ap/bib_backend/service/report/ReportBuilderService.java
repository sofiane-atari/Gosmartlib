package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.ReportRowDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.entity.statistic.StudentClassStatistic;
import edu.ap.bib_backend.jpa.repository.StudentClassStatisticRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.TokenStorageService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ReportBuilderService {

        private final StudentClassStatisticRepository statisticRepository;
        private final UserRepository userRepository;
        private final ReportStatisticsQueryService statisticsQueryService;
        private final TokenStorageService tokenStorageService;

        public ReportBuilderService(
                        StudentClassStatisticRepository statisticRepository,
                        UserRepository userRepository,
                        ReportStatisticsQueryService statisticsQueryService,
                        TokenStorageService tokenStorageService) {
                this.statisticRepository = statisticRepository;
                this.userRepository = userRepository;
                this.statisticsQueryService = statisticsQueryService;
                this.tokenStorageService = tokenStorageService;
        }

        public ClassReportDTO buildReport(
                        OneRosterClassDTO oneRosterClass,
                        List<OneRosterUserDTO> students) {
                List<ReportRowDTO> rows = students.stream()
                                .map(student -> toReportRow(student, oneRosterClass.sourcedId()))
                                .sorted(Comparator
                                                .comparingLong(ReportRowDTO::totalLoans)
                                                .thenComparingLong(ReportRowDTO::returnedLoans)
                                                .reversed())
                                .toList();

                int linkedStudentCount = (int) rows.stream()
                                .filter(row -> row.linkedUserId() != null)
                                .count();

                long totalLoans = rows.stream()
                                .mapToLong(ReportRowDTO::totalLoans)
                                .sum();

                long activeLoans = rows.stream()
                                .mapToLong(ReportRowDTO::activeLoans)
                                .sum();

                double averageLoansPerLinkedStudent = linkedStudentCount == 0
                                ? 0
                                : (double) totalLoans / linkedStudentCount;

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

                List<String> linkedUserIds = rows.stream()
                                .map(ReportRowDTO::linkedUserId)
                                .filter(id -> id != null && !id.isBlank())
                                .toList();

                List<PopularGenreDTO> popularGenres = statisticsQueryService.findPopularGenresForUsers(linkedUserIds);
                List<TopRatedBookReportDTO> topRatedBooks = statisticsQueryService
                                .findTopRatedBooksForUsers(linkedUserIds);

                return new ClassReportDTO(
                                oneRosterClass.sourcedId(),
                                oneRosterClass.title(),
                                rows.size(),
                                linkedStudentCount,
                                totalLoans,
                                activeLoans,
                                round(averageLoansPerLinkedStudent),
                                round(classAverageScore),
                                round(classAverageProgress),
                                popularGenres,
                                topRatedBooks,
                                rows);
        }

        private ReportRowDTO toReportRow(
                        OneRosterUserDTO student,
                        String classSourcedId) {
                StudentClassStatistic statistic = statisticRepository
                                .findByStudentSourcedIdAndClassSourcedId(student.sourcedId(), classSourcedId)
                                .orElse(null);

                Optional<User> linkedUser = userRepository.findByOneRosterSourcedId(student.sourcedId());

                String linkedUserId = linkedUser
                                .map(User::getId)
                                .filter(id -> tokenStorageService.hasValidSmartschoolRefreshToken(id))
                                .orElse(null);

                long totalLoans = linkedUserId == null
                                ? 0
                                : statisticsQueryService.countLoanedBooksForUser(linkedUserId);

                long activeLoans = linkedUserId == null
                                ? 0
                                : statisticsQueryService.countActiveLoanedBooksForUser(linkedUserId);

                long returnedLoans = linkedUserId == null
                                ? 0
                                : statisticsQueryService.countReturnedLoanedBooksForUser(linkedUserId);

                long reviewCount = linkedUserId == null
                                ? 0
                                : statisticsQueryService.countReviewsForUser(linkedUserId);

                if (statistic == null) {
                        return new ReportRowDTO(
                                        student.sourcedId(),
                                        student.displayName(),
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        linkedUserId,
                                        totalLoans,
                                        activeLoans,
                                        returnedLoans,
                                        reviewCount);
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
                                statistic.getTotalTimeMinutes(),
                                linkedUserId,
                                totalLoans,
                                activeLoans,
                                returnedLoans,
                                reviewCount);
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }
}