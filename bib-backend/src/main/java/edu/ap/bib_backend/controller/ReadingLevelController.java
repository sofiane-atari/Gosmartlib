package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;

@RestController
@RequestMapping("/reading-levels")
public class ReadingLevelController {
    private final ReadingLevelRepository readingLevelRepository;

    public ReadingLevelController(ReadingLevelRepository readingLevelRepository) {
        this.readingLevelRepository = readingLevelRepository;
    }

    @GetMapping
    public List<ReadingLevel> getAllReadingLevels() {
        return readingLevelRepository.findAll();
    }
}
