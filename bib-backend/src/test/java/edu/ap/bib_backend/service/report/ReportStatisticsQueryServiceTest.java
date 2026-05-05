package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.report.PopularGenreDTO;
import edu.ap.bib_backend.dto.report.TopRatedBookReportDTO;
import edu.ap.bib_backend.jpa.repository.loan.LoanRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.projection.report.PopularGenreProjection;
import edu.ap.bib_backend.projection.report.TopRatedBookProjection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportStatisticsQueryServiceTest {

    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);

    private final ReportStatisticsQueryService service =
            new ReportStatisticsQueryService(loanRepository, reviewRepository);

    @Test
    void countLoanedBooksForUser_returnsCount() {
        when(loanRepository.countLoanedBooksForUser("user-1")).thenReturn(12L);

        long result = service.countLoanedBooksForUser("user-1");

        assertThat(result).isEqualTo(12L);
        verify(loanRepository).countLoanedBooksForUser("user-1");
    }

    @Test
    void countActiveLoanedBooksForUser_returnsCount() {
        when(loanRepository.countActiveLoanedBooksForUser("user-1")).thenReturn(3L);

        long result = service.countActiveLoanedBooksForUser("user-1");

        assertThat(result).isEqualTo(3L);
        verify(loanRepository).countActiveLoanedBooksForUser("user-1");
    }

    @Test
    void countReturnedLoanedBooksForUser_returnsCount() {
        when(loanRepository.countReturnedLoanedBooksForUser("user-1")).thenReturn(9L);

        long result = service.countReturnedLoanedBooksForUser("user-1");

        assertThat(result).isEqualTo(9L);
        verify(loanRepository).countReturnedLoanedBooksForUser("user-1");
    }

    @Test
    void countReviewsForUser_returnsCount() {
        when(reviewRepository.countReviewsForUser("user-1")).thenReturn(4L);

        long result = service.countReviewsForUser("user-1");

        assertThat(result).isEqualTo(4L);
        verify(reviewRepository).countReviewsForUser("user-1");
    }

    @Test
    void findPopularGenresForUsers_returnsEmptyList_whenUserIdsEmpty() {
        List<PopularGenreDTO> result = service.findPopularGenresForUsers(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(loanRepository);
    }

    @Test
    void findPopularGenresForUsers_mapsProjectionToDto() {
        PopularGenreProjection projection = mock(PopularGenreProjection.class);

        when(projection.getGenreId()).thenReturn(1L);
        when(projection.getName()).thenReturn("Fantasy");
        when(projection.getColor()).thenReturn("#8A1E41");
        when(projection.getLoanCount()).thenReturn(7L);

        when(loanRepository.findPopularGenresForUsers(List.of("user-1")))
                .thenReturn(List.of(projection));

        List<PopularGenreDTO> result = service.findPopularGenresForUsers(List.of("user-1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).genreId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Fantasy");
        assertThat(result.get(0).color()).isEqualTo("#8A1E41");
        assertThat(result.get(0).loanCount()).isEqualTo(7L);
    }

    @Test
    void findTopRatedBooksForUsers_returnsEmptyList_whenUserIdsEmpty() {
        List<TopRatedBookReportDTO> result = service.findTopRatedBooksForUsers(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void findTopRatedBooksForUsers_mapsProjectionToDtoAndRoundsRating() {
        TopRatedBookProjection projection = mock(TopRatedBookProjection.class);

        when(projection.getBookId()).thenReturn(10L);
        when(projection.getTitle()).thenReturn("De Hobbit");
        when(projection.getAuthor()).thenReturn("J.R.R. Tolkien");
        when(projection.getImageUrl()).thenReturn("hobbit.jpg");
        when(projection.getAverageRating()).thenReturn(4.567);
        when(projection.getReviewCount()).thenReturn(5L);

        when(reviewRepository.findTopRatedBooksForUsers(List.of("user-1")))
                .thenReturn(List.of(projection));

        List<TopRatedBookReportDTO> result = service.findTopRatedBooksForUsers(List.of("user-1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookId()).isEqualTo(10L);
        assertThat(result.get(0).title()).isEqualTo("De Hobbit");
        assertThat(result.get(0).author()).isEqualTo("J.R.R. Tolkien");
        assertThat(result.get(0).imageUrl()).isEqualTo("hobbit.jpg");
        assertThat(result.get(0).averageRating()).isEqualTo(4.57);
        assertThat(result.get(0).reviewCount()).isEqualTo(5L);
    }
}