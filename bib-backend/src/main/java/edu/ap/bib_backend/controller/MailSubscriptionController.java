package edu.ap.bib_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.service.MailSubscriptionService;
import edu.ap.bib_backend.service.user.CurrentUserService;

@RestController
@RequestMapping("/subscriptions")
public class MailSubscriptionController {

    private final MailSubscriptionService mailSubscriptionService;
    private final CurrentUserService currentUserService;

    public MailSubscriptionController(
            MailSubscriptionService mailSubscriptionService,
            CurrentUserService currentUserService) {
        this.mailSubscriptionService = mailSubscriptionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/books/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MailSubscriptionDTO subscribe(@PathVariable Long bookId, Authentication authentication) {
        String userId = currentUserService.extractCurrentUserId(authentication);
        return mailSubscriptionService.subscribe(userId, bookId);
    }

    @DeleteMapping("/books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long bookId, Authentication authentication) {
        String userId = currentUserService.extractCurrentUserId(authentication);
        mailSubscriptionService.unsubscribe(userId, bookId);
    }

    @GetMapping("/me")
    public List<MailSubscriptionDTO> mySubscriptions(Authentication authentication) {
        String userId = currentUserService.extractCurrentUserId(authentication);
        return mailSubscriptionService.findSubscriptionsByUser(userId);
    }
}
