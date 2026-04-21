package edu.ap.bib_backend.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import edu.ap.bib_backend.service.book.BookBulkService;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookBulkController {

    private final BookBulkService bbs;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadBooks(@RequestParam("file") MultipartFile file) {
        if (!file.getOriginalFilename().endsWith(".xlsm")) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Only excel files are supported"));
        }

        bbs.importBooks(file);
        return ResponseEntity.ok(Map.of("message", "Books uploaded successfully"));
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        ClassPathResource file = new ClassPathResource("templates/books_template.xlsm");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=books_template.xlsm")
                .body(file);
    }

    // Step 2: SSE validation
    @GetMapping("/upload/validate-stream/{tempFileId}")
    public SseEmitter validateStream(@PathVariable String tempFileId) throws IOException {
        File file = bbs.getTempFile(tempFileId);
        return bbs.validateBooksStream(file);
    }

    // Step 1: Upload file → returns temp ID
    @PostMapping("/upload/temp")
    public ResponseEntity<String> uploadTempFile(@RequestParam("file") MultipartFile file) throws IOException {
        String tempFileId = bbs.storeTempFile(file);
        return ResponseEntity.ok(tempFileId);
    }

}
