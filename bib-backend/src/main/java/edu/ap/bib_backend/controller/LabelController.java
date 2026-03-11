package edu.ap.bib_backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.dto.LabelStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.service.LabelService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    public ResponseEntity<LabelDTO> createLabel(@Valid @RequestBody LabelDTO entity) {

        LabelDTO createdLabel = labelService.createLabel(entity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{name}")
                .buildAndExpand(createdLabel.name()).toUri();

        return ResponseEntity.created(location).body(createdLabel);
    }

    @GetMapping
    public ResponseEntity<PageDTO<LabelDTO>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @ModelAttribute LabelGetOptions options) {

        PageDTO<LabelDTO> pageDTO = labelService.getPage(page, pageSize, options);

        return ResponseEntity.ok(pageDTO);
    }

    // params starting with underscore are preserved params to avoid conflict of a
    // entity with the same name
    @GetMapping("_stats")
    public ResponseEntity<LabelStatsDTO> getLabelStats() {

        return ResponseEntity.ok(labelService.getAllLabelStats());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        
        labelService.deleteById(name);
        return ResponseEntity.noContent().build();
    }
}