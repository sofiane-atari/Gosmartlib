package edu.ap.bib_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;

@RestController
@RequestMapping("/oneroster")
public class OneRosterSyncController {

    private final OneRosterUserSyncService syncService;

    public OneRosterSyncController(OneRosterUserSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> syncUsersNow() {
        syncService.syncAllSchools();
        return ResponseEntity.ok().build();
    }
}
