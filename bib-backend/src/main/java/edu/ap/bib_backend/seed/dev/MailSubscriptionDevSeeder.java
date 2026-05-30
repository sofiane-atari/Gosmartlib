package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.service.MailSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MailSubscriptionDevSeeder {

    private final MailSubscriptionService mailSubscriptionService;

    public MailSubscriptionDevSeeder(MailSubscriptionService mailSubscriptionService) {
        this.mailSubscriptionService = mailSubscriptionService;
    }

    public MailSubscriptionSeedResult seed(List<String> studentIds, List<Long> bookIds) {
        if (studentIds.isEmpty() || bookIds.isEmpty()) {
            log.info("Geen studenten of boeken gevonden, mailabonnement seeding overgeslagen");
            return new MailSubscriptionSeedResult(0, List.of());
        }

        int subscriptionsCreated = 0;
        List<SubscriptionKey> subscriptions = new ArrayList<>();
        int maxStudents = Math.min(5, studentIds.size());
        int maxBooks = Math.min(2, bookIds.size());

        for (int i = 0; i < maxStudents; i++) {
            String studentId = studentIds.get(i);
            for (int j = 0; j < maxBooks; j++) {
                Long bookId = bookIds.get((i + j) % bookIds.size());
                try {
                    mailSubscriptionService.subscribe(studentId, bookId);
                    subscriptionsCreated++;
                    subscriptions.add(new SubscriptionKey(studentId, bookId));
                    log.debug("Mailabonnement aangemaakt: student={}, boek={}", studentId, bookId);
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("al geabonneerd")) {
                        log.debug("Mailabonnement overgeslagen (bestaat al): student={}, boek={}", studentId, bookId);
                    } else {
                        log.warn("Kon mailabonnement niet aanmaken: student={}, boek={}: {}",
                                studentId, bookId, e.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("Kon mailabonnement niet aanmaken: student={}, boek={}: {}",
                            studentId, bookId, e.getMessage());
                }
            }
        }

        log.info("Mailabonnement seeding voltooid: {} abonnementen aangemaakt", subscriptionsCreated);
        return new MailSubscriptionSeedResult(subscriptionsCreated, subscriptions);
    }

    public record MailSubscriptionSeedResult(int count, List<SubscriptionKey> subscriptions) {}

    public record SubscriptionKey(String userId, Long bookId) {}
}