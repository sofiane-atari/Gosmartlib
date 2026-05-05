package edu.ap.bib_backend.mapper;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.dto.report.ClassReportDTO;
import edu.ap.bib_backend.dto.report.ReportRowDTO;
import edu.ap.bib_backend.jpa.entity.statistic.StudentClassStatistic;
import edu.ap.bib_backend.jpa.repository.StudentClassStatisticRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportBuilderService {

    private final StudentClassStatisticRepository statisticRepository;

    public ReportBuilderService(StudentClassStatisticRepository statisticRepository) {
        this.statisticRepository = statisticRepository;
    }

    public ClassReportDTO buildReport(
            OneRosterClassDTO oneRosterClass,
            List<OneRosterUserDTO> students) {

        List<ReportRowDTO> rows = students.stream()
                .map(student -> toReportRow(student, oneRosterClass.sourcedId()))
                .toList();

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

        return new ClassReportDTO(
                oneRosterClass.sourcedId(),
                oneRosterClass.title(),
                rows.size(),
                round(classAverageScore),
                round(classAverageProgress),
                rows);
    }

    private ReportRowDTO toReportRow(
            OneRosterUserDTO student,
            String classSourcedId) {
        StudentClassStatistic statistic = statisticRepository
                .findByStudentSourcedIdAndClassSourcedId(student.sourcedId(), classSourcedId)
                .orElse(null);

        if (statistic == null) {
            return new ReportRowDTO(
                    student.sourcedId(),
                    student.displayName(),
                    0,
                    0,
                    0,
                    0,
                    0);
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
                statistic.getTotalTimeMinutes());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}