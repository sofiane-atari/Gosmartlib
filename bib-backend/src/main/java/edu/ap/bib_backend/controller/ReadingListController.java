package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AddBookDTO;
import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.service.ReadingListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/reading-lists")
public class ReadingListController {
    private final ReadingListService readingListService;

    public ReadingListController(ReadingListService readingListService) {
        this.readingListService = readingListService;
    }

    @GetMapping
    public ResponseEntity<List<ReadingList>> getAllReadingLists(Principal principal) {
        return ResponseEntity.ok(readingListService.getAllLists(requireUserId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReadingList> getReadingListById(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(readingListService.getListById(requireUserId(principal), id));
    }

    @PostMapping
    public ResponseEntity<ReadingList> createReadingList(@RequestBody ReadingListDTO dto, Principal principal) {
        ReadingList created = readingListService.createReadingList(requireUserId(principal), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReadingList(@PathVariable("id") Long id, Principal principal) {
        readingListService.deleteReadingList(requireUserId(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listId}/books")
    public ResponseEntity<ReadingListBook> addBookToList(@PathVariable("listId") Long listId,
            @RequestBody AddBookDTO dto,
            Principal principal) {
        ReadingListBook addedBook = readingListService.addBookToList(requireUserId(principal), listId, dto.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(addedBook);
    }

    @PostMapping("/favorites/books")
    public ResponseEntity<ReadingListBook> addBookToFavorites(@RequestBody AddBookDTO dto, Principal principal) {
        ReadingListBook addedBook = readingListService.addBookToFavorites(requireUserId(principal), dto.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(addedBook);
    }

    @DeleteMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromList(@PathVariable("listId") Long listId,
            @PathVariable("bookId") Long bookId,
            Principal principal) {
        readingListService.removeBookFromList(requireUserId(principal), listId, bookId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    private String requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet geauthenticeerd");
        }
        return principal.getName();
    }
}
