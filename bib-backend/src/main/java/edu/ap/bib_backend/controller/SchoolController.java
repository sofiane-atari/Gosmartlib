package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.service.SchoolService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    public ResponseEntity<List<SchoolDTO>> getAll() {
        return ResponseEntity.ok(schoolService.getAll());
    }
}
