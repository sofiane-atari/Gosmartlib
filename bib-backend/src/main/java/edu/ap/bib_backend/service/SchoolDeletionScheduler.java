package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SchoolDeletionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SchoolDeletionScheduler.class);

    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final long gracePeriodDays;

    public SchoolDeletionScheduler(
            SchoolRepository schoolRepository,
            SchoolService schoolService,
            @Value("${school.deletion.grace-period-days:30}") long gracePeriodDays) {
        this.schoolRepository = schoolRepository;
        this.schoolService = schoolService;
        this.gracePeriodDays = gracePeriodDays;
    }

    @Scheduled(cron = "${school.deletion.check-cron:0 0 2 * * *}")
    public void processScheduledDeletions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(gracePeriodDays);

        List<School> expired = schoolRepository.findByStatusAndMarkedForDeletionAtBefore(
                SchoolStatus.MARKED_FOR_DELETION, threshold);

        if (expired.isEmpty()) {
            return;
        }

        logger.info("Processing {} school(s) scheduled for permanent deletion", expired.size());

        for (School school : expired) {
            try {
                logger.info("Permanently deleting school '{}' (id={})", school.getName(), school.getId());
                schoolService.purge(school.getId());
            } catch (Exception e) {
                logger.error("Failed to purge school '{}' (id={}): {}", school.getName(), school.getId(), e.getMessage());
            }
        }

        logger.info("Finished processing scheduled school deletions");
    }
}
