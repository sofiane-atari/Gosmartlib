package edu.ap.bib_backend.jpa.entity.statistic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "student_class_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_class_statistics_student_class", columnNames = { "student_sourced_id",
                "class_sourced_id" })
})
@Getter
@Setter
public class StudentClassStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_sourced_id", nullable = false)
    private String studentSourcedId;

    @Column(name = "class_sourced_id", nullable = false)
    private String classSourcedId;

    @Column(nullable = false)
    private int totalItems;

    @Column(nullable = false)
    private int completedItems;

    @Column(nullable = false)
    private double averageScore;

    @Column(nullable = false)
    private int totalTimeMinutes;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}