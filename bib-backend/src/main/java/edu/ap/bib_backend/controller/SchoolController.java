package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.service.SchoolService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @PostMapping
    public ResponseEntity<SchoolDTO> create(@Valid @RequestBody SchoolDTO schoolDTO) {
        SchoolDTO createdSchool = schoolService.createSchool(schoolDTO);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{domainName}")
                .buildAndExpand(createdSchool.domainName())
                .toUri();

        return ResponseEntity.created(location).body(createdSchool);
    }

    @DeleteMapping("/{domainName}")
    public ResponseEntity<Void> delete(@PathVariable String domainName) {
        schoolService.deleteSchool(domainName);
        return ResponseEntity.noContent().build();
    }
}

