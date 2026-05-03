package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AddBookDTO;
import edu.ap.bib_backend.dto.OneRosterAudienceOptionsDto;
import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.ReadingListService;
import edu.ap.bib_backend.service.oneroster.OneRosterAudienceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import edu.ap.bib_backend.dto.ReadingListShareDTO;

@RestController
@RequestMapping("/reading-lists")
public class ReadingListController {
    private final ReadingListService readingListService;
    private final OneRosterAudienceService oneRosterAudienceService;
    private final JwtService jwtService;

    public ReadingListController(ReadingListService readingListService,
            OneRosterAudienceService oneRosterAudienceService,
            JwtService jwtService) {
        this.readingListService = readingListService;
        this.oneRosterAudienceService = oneRosterAudienceService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<ReadingList>> getAllReadingLists(Principal principal) {
        return ResponseEntity.ok(readingListService.getAllLists(requireUserId(principal)));
    }

    @GetMapping("/shared/{publicShareId}")
    public ResponseEntity<ReadingList> getSharedReadingList(
            @PathVariable String publicShareId,
            Principal principal) {

        requireUserId(principal); // verplicht ingelogd zijn
        return ResponseEntity.ok(readingListService.getSharedList(publicShareId));
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

    @PostMapping("/{id}/share")
    public ResponseEntity<ReadingListShareDTO> shareReadingList(
            @PathVariable("id") Long id,
            @RequestParam(required = false) String frontendBaseUrl,
            Principal principal) {

        return ResponseEntity.ok(
                readingListService.enableSharing(
                        requireUserId(principal),
                        id,
                        frontendBaseUrl
                )
        );
    }


    @DeleteMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromList(@PathVariable("listId") Long listId,
            @PathVariable("bookId") Long bookId,
            Principal principal) {
        readingListService.removeBookFromList(requireUserId(principal), listId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher/audience-options")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<OneRosterAudienceOptionsDto> getTeacherAudienceOptions(
            HttpServletRequest request,
            Principal principal) {
        String schoolCode = requireSchoolCodeFromAuthentication(request);
        String classCode = readClassCodeFromAuthentication();
        String userId = requireUserId(principal);
        return ResponseEntity.ok(oneRosterAudienceService.getAudienceOptions(userId, schoolCode, classCode));
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

    private String requireSchoolCodeFromAuthentication(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // JWT filter zet audience in Authentication.details als map.
            if (authentication.getDetails() instanceof Map<?, ?> details) {
                String schoolCodeFromDetails = asNonBlankString(details.get("schoolCode"));
                if (schoolCodeFromDetails != null) {
                    return schoolCodeFromDetails;
                }
            }

            // OAuth2 login-flow bewaart claims in principal attributes.
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
                String schoolCodeFromClaims = asNonBlankString(oauthPrincipal.getAttribute("schoolCode"));
                if (schoolCodeFromClaims != null) {
                    return schoolCodeFromClaims;
                }
            }
        }

        // Fallback: lees claim direct uit Bearer token.
        String bearerToken = extractBearerToken(request);
        if (bearerToken != null && jwtService.isValid(bearerToken)) {
            String schoolCodeFromToken = asNonBlankString(jwtService.extractClaim(bearerToken, "schoolCode"));
            if (schoolCodeFromToken != null) {
                return schoolCodeFromToken;
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geen schoolcontext beschikbaar");
    }

    private String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private String asNonBlankString(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return null;
    }

    private String readClassCodeFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getDetails() instanceof Map<?, ?> details) {
                String classCodeFromDetails = asNonBlankString(details.get("classCode"));
                if (classCodeFromDetails != null) {
                    return classCodeFromDetails;
                }
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
                String classCodeFromClaims = asNonBlankString(oauthPrincipal.getAttribute("classCode"));
                if (classCodeFromClaims != null) {
                    return classCodeFromClaims;
                }
            }
        }

        return null;
    }
}
