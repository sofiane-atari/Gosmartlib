package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AddBookDTO;
import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.service.ReadingListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-lists")
public class ReadingListController {
    private final ReadingListService readingListService;

    public ReadingListController(ReadingListService readingListService) {
        this.readingListService = readingListService;
    }

    @GetMapping
    public ResponseEntity<List<ReadingList>> getAllReadingLists() {
        return ResponseEntity.ok(readingListService.getAllLists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReadingList> getReadingListById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(readingListService.getListById(id));
    }

    @PostMapping
    public ResponseEntity<ReadingList> createReadingList(@RequestBody ReadingListDTO dto) {
        ReadingList created = readingListService.createReadingList(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReadingList(@PathVariable("id") Long id) {
        readingListService.deleteReadingList(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listId}/books")
    public ResponseEntity<ReadingListBook> addBookToList(@PathVariable("listId") Long listId,
            @RequestBody AddBookDTO dto) {
        ReadingListBook addedBook = readingListService.addBookToList(listId, dto.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(addedBook);
    }

    @PostMapping("/favorites/books")
    public ResponseEntity<ReadingListBook> addBookToFavorites(@RequestBody AddBookDTO dto) {
        ReadingListBook addedBook = readingListService.addBookToFavorites(dto.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(addedBook);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
