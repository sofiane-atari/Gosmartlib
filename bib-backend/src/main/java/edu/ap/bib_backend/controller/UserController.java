package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.SwitchCampusRequestDto;
import edu.ap.bib_backend.dto.UserReviewAccessUpdateDto;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<AuthUserDto>> getUsers(@ModelAttribute UserGetOptions options) {
        return ResponseEntity.ok(userService.getUsers(options));
    }

    @PatchMapping("/me/campus")
    public ResponseEntity<AuthUserDto> switchCurrentUserCampus(
            @RequestBody SwitchCampusRequestDto payload,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.switchCurrentUserCampus(authentication, payload.campusId()));
    }

    @PatchMapping("/{userId}/review-access")
    public ResponseEntity<AuthUserDto> updateReviewAccess(
            @PathVariable String userId,
            @RequestBody UserReviewAccessUpdateDto payload,
            Authentication authentication
    ) {
        if (!currentUserService.isModerator(authentication)) {
            throw new IllegalStateException("Alleen moderators kunnen reviewtoegang aanpassen.");
        }

        return ResponseEntity.ok(userService.setReviewBlocked(userId, payload.blocked()));
    }
}