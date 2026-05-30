package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.service.CampusService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campuses")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CampusDTO> getCampusesBySchool(@RequestParam Long schoolId) {
        return campusService.getCampusesBySchool(schoolId);
    }

    @GetMapping("/me")
    public List<CampusDTO> getCampusesForCurrentUserSchool(Authentication authentication) {
        return campusService.getCampusesForCurrentUserSchool(authentication);
    }

    @GetMapping("/{id}")
    public CampusDTO getCampus(@PathVariable Long id, Authentication authentication) {
        return campusService.getCampus(id, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusDTO createCampus(@RequestParam Long schoolId, @RequestBody CampusDTO dto) {
        return campusService.createCampus(schoolId, dto);
    }

    @PutMapping("/{id}")
    public CampusDTO updateCampus(@PathVariable Long id, @RequestBody CampusDTO dto) {
        return campusService.updateCampus(id, dto);
    }
}