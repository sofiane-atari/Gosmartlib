package edu.ap.bib_backend.projection.report;

import java.time.LocalDateTime;

public interface SystemSchoolActivityProjection {
    Long getSchoolId();

    String getSchoolName();

    Long getTotalStudents();

    Long getLinkedStudents();

    Long getTotalLoans();

    Long getActiveLoans();

    // New: expose last sync timestamp from schools.last_sync_at
    LocalDateTime getLastSyncAt();
}
