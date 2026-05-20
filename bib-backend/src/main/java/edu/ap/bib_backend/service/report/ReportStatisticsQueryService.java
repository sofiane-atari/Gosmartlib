package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportStatisticsQueryService {

    private final LoanRepository loanRepository;
    private final ReviewRepository reviewRepository;

    public ReportStatisticsQueryService(
            LoanRepository loanRepository,
            ReviewRepository reviewRepository) {
        this.loanRepository = loanRepository;
        this.reviewRepository = reviewRepository;
    }

    public long countLoanedBooksForUser(String userId) {
        return loanRepository.countLoanedBooksForUser(userId);
    }

    public long countActiveLoanedBooksForUser(String userId) {
        return loanRepository.countActiveLoanedBooksForUser(userId);
    }

    public long countReturnedLoanedBooksForUser(String userId) {
        return loanRepository.countReturnedLoanedBooksForUser(userId);
    }

    public long countReviewsForUser(String userId) {
        return reviewRepository.countReviewsForUser(userId);
    }

    public List<PopularGenreDTO> findPopularGenresForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return loanRepository.findPopularGenresForUsers(userIds)
                .stream()
                .map(item -> new PopularGenreDTO(
                        item.getGenreId(),
                        item.getName(),
                        item.getColor(),
                        item.getLoanCount() == null ? 0 : item.getLoanCount()))
                .toList();
    }

    public List<TopRatedBookReportDTO> findTopRatedBooksForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return reviewRepository.findTopRatedBooksForUsers(userIds)
                .stream()
                .map(item -> new TopRatedBookReportDTO(
                        item.getBookId(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getImageUrl(),
                        round(item.getAverageRating() == null ? 0 : item.getAverageRating()),
                        item.getReviewCount() == null ? 0 : item.getReviewCount()))
                .toList();
    }

    public List<PopularGenreDTO> findPopularGenresForSchool(Long schoolId, Long campusId, Pageable pageable) {
        return loanRepository.findPopularGenresForSchool(schoolId, campusId, pageable)
                .stream()
                .map(item -> new PopularGenreDTO(
                        item.getGenreId(),
                        item.getName(),
                        item.getColor(),
                        item.getLoanCount() == null ? 0 : item.getLoanCount()))
                .toList();
    }

    public List<TopRatedBookReportDTO> findTopRatedBooksForSchool(Long schoolId, Long campusId, Pageable pageable) {
        return reviewRepository.findTopRatedBooksForSchool(schoolId, campusId, pageable)
                .stream()
                .map(item -> new TopRatedBookReportDTO(
                        item.getBookId(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getImageUrl(),
                        round(item.getAverageRating() == null ? 0 : item.getAverageRating()),
                        item.getReviewCount() == null ? 0 : item.getReviewCount()))
                .toList();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
