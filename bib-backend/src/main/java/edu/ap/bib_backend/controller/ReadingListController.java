package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.service.ReadingListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = { "https://gosmartlib.be", "https://api.gosmartlib.be", "http://localhost:4200" })

@RestController
@RequestMapping("/api/reading-lists")
public class ReadingListController {
    private final ReadingListService readingListService;

    public ReadingListController(ReadingListService readingListService) {
        this.readingListService = readingListService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReadingList>> getReadingList(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(readingListService.getListByUser(userId));
    }

    @PostMapping
    public ResponseEntity<ReadingList> createReadingList(@RequestBody ReadingListDTO dto) {
        ReadingList created = readingListService.createReadingList(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
