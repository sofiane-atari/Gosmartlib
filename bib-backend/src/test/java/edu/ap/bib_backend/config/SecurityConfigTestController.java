package edu.ap.bib_backend.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityConfigTestController {

    @GetMapping("/test/probe")
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

    @GetMapping("/users/test")
    public String users() {
        return "ok";
    }

    @GetMapping("/tags/test")
    public String tags() {
        return "ok";
    }

    @PostMapping("/roles/assign-admin")
    public String assignAdmin() {
        return "ok";
    }

    @PostMapping("/roles/revoke-admin")
    public String revokeAdmin() {
        return "ok";
    }

    @PostMapping("/roles/assign-librarian")
    public String assignLibrarian() {
        return "ok";
    }

    @PostMapping("/roles/revoke-librarian")
    public String revokeLibrarian() {
        return "ok";
    }

    @GetMapping("/reports/test")
    public String reports() {
        return "ok";
    }

    @GetMapping("/book-copies/test")
    public String bookCopies() {
        return "ok";
    }

    @GetMapping("/loans/me")
    public String loansMe() {
        return "ok";
    }

    @PostMapping("/loans/test")
    public String loansPost() {
        return "ok";
    }

    @GetMapping("/reading-lists/test")
    public String readingLists() {
        return "ok";
    }

    @GetMapping("/subscriptions/test")
    public String subscriptions() {
        return "ok";
    }

    @GetMapping("/schools/1/catalog-sidebar-settings")
    public String catalogSidebar() {
        return "ok";
    }

    @PutMapping("/schools/1/catalog-sidebar-settings")
    public String updateCatalogSidebar() {
        return "ok";
    }

    @GetMapping("/schools/dev-mode-enabled")
    public String devModeEnabled() {
        return "ok";
    }

    @GetMapping("/campuses/test")
    public String campuses() {
        return "ok";
    }

    @GetMapping("/lesson-tips/test")
    public String lessonTips() {
        return "ok";
    }

    @GetMapping("/reading-levels/test")
    public String readingLevels() {
        return "ok";
    }

    @GetMapping("/reviews/test")
    public String reviews() {
        return "ok";
    }
}