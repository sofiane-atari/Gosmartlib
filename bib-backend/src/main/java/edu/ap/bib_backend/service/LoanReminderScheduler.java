package edu.ap.bib_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoanReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanReminderScheduler.class);

    private final LoanReminderService loanReminderService;

    public LoanReminderScheduler(LoanReminderService loanReminderService) {
        this.loanReminderService = loanReminderService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void processReminders() {
        log.info("Starting loan reminder processing");

        try {
            loanReminderService.sendUpcomingReturnReminders();
        } catch (Exception e) {
            log.error("Error processing upcoming return reminders", e);
        }

        try {
            loanReminderService.sendOverdueUserReminders();
        } catch (Exception e) {
            log.error("Error processing overdue user reminders", e);
        }

        try {
            loanReminderService.sendOverdueLibrarianAlerts();
        } catch (Exception e) {
            log.error("Error processing librarian alerts", e);
        }

        log.info("Finished loan reminder processing");
    }
}
