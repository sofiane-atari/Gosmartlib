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
import org.springframework.web.bind.annotation.PutMapping;
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
    public List<SchoolDTO> getAll() {
        return schoolService.getAll();
    }

    @PostMapping
    public SchoolDTO create(@Valid @RequestBody SchoolDTO dto) {
        return schoolService.create(dto);
    }

    @PutMapping("/{id}")
    public SchoolDTO update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolDTO dto) {
        return schoolService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        schoolService.delete(id);
    }
}
