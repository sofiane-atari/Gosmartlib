package edu.ap.bib_backend.jpa.entity.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.entity.User;

@Entity
@Table(
    name = "review_reports",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "reported_by_user_id"})
    }
)
@Getter
@Setter
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private User reportedBy;

    public ReviewReport() {
    }

    public ReviewReport(Review review, User reportedBy) {
        this.review = review;
        this.reportedBy = reportedBy;
        this.reportedAt = LocalDateTime.now();
    }
}