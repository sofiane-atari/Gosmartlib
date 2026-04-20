package edu.ap.bib_backend.controller.didactic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.didactic.CreateDidacticMaterialRequestDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.service.didactic.DidacticMaterialService;
import edu.ap.bib_backend.service.review.CurrentUserService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/didactic-materials")
@PreAuthorize("hasAnyRole('STUDENT','TEACHER','LIBRARIAN','ADMIN')")
public class DidacticMaterialController {

    private final DidacticMaterialService didacticMaterialService;
    private final CurrentUserService currentUserService;

    public DidacticMaterialController(DidacticMaterialService didacticMaterialService, CurrentUserService currentUserService) {
        this.didacticMaterialService = didacticMaterialService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<DidacticMaterialResponseDto>> getAllMaterials() {
        return ResponseEntity.ok(didacticMaterialService.getAllMaterials());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DidacticMaterialResponseDto> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(didacticMaterialService.getMaterialById(id));
    }

    @PostMapping
    public ResponseEntity<DidacticMaterialResponseDto> createMaterial(
        @Valid @RequestBody CreateDidacticMaterialRequestDto request,
        Authentication authentication
    ) {
        String currentUserId = currentUserService.extractCurrentUserId(authentication);
        DidacticMaterialResponseDto createdMaterial =
                didacticMaterialService.createMaterial(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdMaterial);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DidacticMaterialResponseDto> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody CreateDidacticMaterialRequestDto request,
            Authentication authentication
    ) {
        DidacticMaterialResponseDto updatedMaterial =
                didacticMaterialService.updateMaterial(id, request);

        return ResponseEntity.ok(updatedMaterial);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        didacticMaterialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

}