package edu.ap.bib_backend.controller;


import edu.ap.bib_backend.service.BookBulkService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<String> uploadBooks(@RequestParam("file") MultipartFile file) {
        bbs.importBooks(file);
        return ResponseEntity.ok("Books uploaded succesfully");
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate(){
        ClassPathResource file = new ClassPathResource("templates/books_template.xlsx");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=books_template.xlsx")
            .body(file);
    }

}
