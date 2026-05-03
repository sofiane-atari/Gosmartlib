package edu.ap.bib_backend.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityConfigTestController {

    @GetMapping("/auth/me")
    public String authMe() {
        return "ok";
    }

    @GetMapping("/books/test")
    public String books() {
        return "ok";
    }

    @GetMapping("/genres/test")
    public String genres() {
        return "ok";
    }
}