package edu.ap.bib_backend.jpa.repository.review;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.ap.bib_backend.jpa.entity.review.Review;
import edu.ap.bib_backend.jpa.enums.ReviewModerationStatus;
import edu.ap.bib_backend.projection.report.TopRatedBookProjection;

public interface ReviewRepository extends JpaRepository<Review, Long> {

        @Query("""
                            select r
                            from Review r
                            where r.user.id = :userId
                            order by
                                case
                                    when r.moderationStatus = edu.ap.bib_backend.jpa.enums.ReviewModerationStatus.FLAGGED then 0
                                    else 1
                                end,
                                r.createdAt desc
                        """)
        Page<Review> findCurrentUserReviewsWithFlaggedFirst(
                        @Param("userId") String userId,
                        Pageable pageable);

        Page<Review> findByUserIdAndModerationStatusNot(
                        String userId,
                        ReviewModerationStatus moderationStatus,
                        Pageable pageable);

        boolean existsByBookIdAndUserId(Long bookId, String userId);

        List<Review> findByBookId(Long bookId);

        Page<Review> findByUserId(String userId, Pageable pageable);

        void deleteByBookId(Long bookId);

        Page<Review> findByModerationStatus(ReviewModerationStatus moderationStatus, Pageable pageable);

        long countByModerationStatus(ReviewModerationStatus moderationStatus);

        long countByModerationStatusAndReportCountGreaterThanEqual(
                        ReviewModerationStatus moderationStatus,
                        int reportCount);

        @Query("""
                            SELECT r
                            FROM Review r
                            WHERE r.book.id = :bookId
                            AND r.moderationStatus IN :statuses
                            ORDER BY r.createdAt DESC
                        """)
        List<Review> findVisibleReviewsByBookId(
                        @Param("bookId") Long bookId,
                        @Param("statuses") List<ReviewModerationStatus> statuses);

        @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.book.id = :bookId AND r.moderationStatus IN ('ACTIVE','KEPT')")
        Double getAverageRatingForBook(@Param("bookId") Long bookId);

        @Query(value = """
                        select count(*)
                        from reviews r
                        where r.user_id = :userId
                        """, nativeQuery = true)
        long countReviewsForUser(@Param("userId") String userId);

        @Query(value = """
                        select b.id as bookId,
                               b.title as title,
                               b.author as author,
                               b.image_url as imageUrl,
                               avg(r.rating) as averageRating,
                               count(r.id) as reviewCount
                        from reviews r
                        join books b on b.id = r.book_id
                        where r.user_id in (:userIds)
                        and r.moderation_status in ('ACTIVE', 'KEPT')
                        group by b.id, b.title, b.author, b.image_url
                        order by averageRating desc, reviewCount desc, b.title asc
                        limit 6
                        """, nativeQuery = true)
        List<TopRatedBookProjection> findTopRatedBooksForUsers(@Param("userIds") List<String> userIds);
}
