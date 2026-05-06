package edu.ap.bib_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.UserReviewAccessUpdateDto;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.user.UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final edu.ap.bib_backend.service.TokenStorageService tokenStorageService;

    public UserController(UserService userService, CurrentUserService currentUserService, edu.ap.bib_backend.service.TokenStorageService tokenStorageService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.tokenStorageService = tokenStorageService;
    }

    @GetMapping
    public ResponseEntity<List<AuthUserDto>> getUsers(@ModelAttribute UserGetOptions options) {
        return ResponseEntity.ok(userService.getUsers(options));
    }

    @GetMapping("/has-refresh-token")
    public ResponseEntity<Boolean> hasRefreshToken(@RequestParam String userId) {
        return ResponseEntity.ok(tokenStorageService.hasValidSmartschoolRefreshToken(userId));
    }

    @PatchMapping("/{userId}/review-access")
    public ResponseEntity<AuthUserDto> updateReviewAccess(
            @PathVariable String userId,
            @RequestBody UserReviewAccessUpdateDto payload,
            Authentication authentication) {

        if (!currentUserService.isModerator(authentication)) {
            throw new IllegalStateException("Alleen moderators kunnen reviewtoegang aanpassen.");
        }

        return ResponseEntity.ok(userService.setReviewBlocked(userId, payload.blocked()));
    }
}