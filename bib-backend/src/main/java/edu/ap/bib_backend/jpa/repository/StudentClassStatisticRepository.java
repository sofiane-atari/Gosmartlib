package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.statistic.StudentClassStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentClassStatisticRepository extends JpaRepository<StudentClassStatistic, Long> {

    Optional<StudentClassStatistic> findByStudentSourcedIdAndClassSourcedId(
            String studentSourcedId,
            String classSourcedId);

    List<StudentClassStatistic> findByClassSourcedId(String classSourcedId);

    List<StudentClassStatistic> findByClassSourcedIdIn(List<String> classSourcedIds);
}
