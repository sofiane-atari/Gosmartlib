package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TeacherStatsDTO;
import edu.ap.bib_backend.dto.UserReviewAccessUpdateDto;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/search-external")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<AuthUserDto>> searchExternalUsers(
            Authentication authentication,
            @RequestParam Long schoolId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        return ResponseEntity.ok(userService.searchExternalUsers(authentication, schoolId, firstName, lastName));
    }

    @GetMapping("/teachers/stats")
    public ResponseEntity<TeacherStatsDTO> getTeacherStats(Authentication authentication) {
        return ResponseEntity.ok(userService.getTeacherStats(authentication));
    }

    @GetMapping("/teachers")
    public ResponseEntity<PageDTO<AuthUserDto>> getTeachers(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean excludeLibrarians,
            @RequestParam(defaultValue = "true") boolean excludeAdmins,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getTeachers(authentication, excludeLibrarians, excludeAdmins, page, pageSize, search));
    }

    @GetMapping("/by-school/{schoolId}")
    public ResponseEntity<List<AuthUserDto>> getUsersBySchoolId(
            @PathVariable Long schoolId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getUsersBySchoolId(authentication, schoolId));
    }

    @PatchMapping("/{userId}/review-access")
    public ResponseEntity<AuthUserDto> updateReviewAccess(
            @PathVariable String userId,
            @RequestBody UserReviewAccessUpdateDto payload,
            Authentication authentication) {
        if (!currentUserService.isModerator(authentication)) {
            throw new AccessDeniedException("Alleen moderators kunnen reviewtoegang aanpassen.");
        }

        return ResponseEntity.ok(userService.setReviewBlocked(userId, payload.blocked()));
    }
}
