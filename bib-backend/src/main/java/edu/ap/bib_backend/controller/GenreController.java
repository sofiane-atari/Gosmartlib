package edu.ap.bib_backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.options.GenreGetOptions;
import edu.ap.bib_backend.service.GenreService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/genres")
public class GenreController {


    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @PostMapping
    public ResponseEntity<GenreDTO> createGenre(@Valid @RequestBody GenreDTO entity) {
        GenreDTO createdGenre = genreService.createGenre(entity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGenre.id())
                .toUri();

        return ResponseEntity.created(location).body(createdGenre);
    }

    @GetMapping
    public ResponseEntity<PageDTO<GenreDTO>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @ModelAttribute GenreGetOptions options) {

        PageDTO<GenreDTO> pageDTO = genreService.getPage(page, pageSize, options);

        return ResponseEntity.ok(pageDTO);
    }

    @GetMapping("/all")
    public List<GenreDTO> getAllGenres() {
        return genreService.getAllGenres();
    }

    // params starting with underscore are preserved params to avoid conflict of a
    // entity with the same name
    @GetMapping("/_stats")
    public ResponseEntity<GenreStatsDTO> getGenreStats() {

        return ResponseEntity.ok(genreService.getAllGenreStats());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        genreService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreDTO> updateGenre(@PathVariable Long id, @Valid @RequestBody GenreDTO inputDTO) {

        GenreDTO outputDTO = genreService.update(id, inputDTO);
        return ResponseEntity.ok(outputDTO);
    }
}