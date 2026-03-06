package edu.ap.bib_backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.service.LabelService;

import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/labels")
@CrossOrigin(origins = { "https://frontend.localhost/", "https://gosmartlib.be/", "http://localhost:4200/" })
public class LabelController {

    private final LabelService labelService;    

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    public ResponseEntity<LabelDTO> createLabel(@RequestBody LabelDTO entity) {
        
        LabelDTO createdLabel = labelService.createLabel(entity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{name}")
                .buildAndExpand(createdLabel.name()).toUri();

        return ResponseEntity.created(location).body(createdLabel);
    }
    
}