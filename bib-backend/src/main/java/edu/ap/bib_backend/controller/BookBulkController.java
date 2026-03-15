package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.FileValidationResult;
import edu.ap.bib_backend.service.BookBulkService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookBulkController {

    private final BookBulkService bbs;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadBooks(@RequestParam("file") MultipartFile file) {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Only excel files are supported"));
        }

        bbs.importBooks(file);
        return ResponseEntity.ok(Map.of("message", "Books uploaded successfully"));
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        ClassPathResource file = new ClassPathResource("templates/books_template.xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=books_template.xlsx")
                .body(file);
    }

    @PostMapping("/upload/validate")
    public ResponseEntity<FileValidationResult> validate(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(bbs.validateBooks(file));
    }

}
