package edu.ap.bib_backend.event;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SchoolCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(SchoolCreatedEventListener.class);

    private final SchoolRepository schoolRepository;
    private final OneRosterUserSyncService syncService;

    public SchoolCreatedEventListener(SchoolRepository schoolRepository,
                                       OneRosterUserSyncService syncService) {
        this.schoolRepository = schoolRepository;
        this.syncService = syncService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSchoolCreated(SchoolCreatedEvent event) {
        School school = schoolRepository.findById(event.schoolId()).orElse(null);
        if (school == null) {
            log.error("SchoolCreatedEvent received for unknown school id: {}", event.schoolId());
            return;
        }

        log.info("School created event received for school id={} name='{}', starting user sync",
                school.getId(), school.getName());

        try {
            OneRosterUserSyncService.SyncResult result = syncService.syncSchoolUsers(school);
            log.info("User sync completed for school id={} name='{}': fetched={}, saved={}, skipped={}",
                    school.getId(), school.getName(),
                    result.fetched(), result.saved(), result.skipped());
        } catch (Exception e) {
            log.error("User sync failed for school id={} name='{}'",
                    school.getId(), school.getName(), e);
        }
    }
}
