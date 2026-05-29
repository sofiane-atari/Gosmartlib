package edu.ap.bib_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanReminderSchedulerTest {

    @Mock
    private LoanReminderService loanReminderService;

    private LoanReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LoanReminderScheduler(loanReminderService);
    }

    @Test
    void Given_SchedulerTriggered_When_ProcessReminders_Then_CallsAllThreeReminderMethods() {
        scheduler.processReminders();

        verify(loanReminderService).sendUpcomingReturnReminders();
        verify(loanReminderService).sendOverdueUserReminders();
        verify(loanReminderService).sendOverdueLibrarianAlerts();
    }

    @Test
    void Given_UpcomingReturnRemindersFails_When_ProcessReminders_Then_ContinuesWithOtherReminders() {
        doThrow(new RuntimeException("Upcoming error")).when(loanReminderService).sendUpcomingReturnReminders();

        scheduler.processReminders();

        verify(loanReminderService).sendOverdueUserReminders();
        verify(loanReminderService).sendOverdueLibrarianAlerts();
    }

    @Test
    void Given_AllRemindersFail_When_ProcessReminders_Then_DoesNotThrow() {
        doThrow(new RuntimeException("Error 1")).when(loanReminderService).sendUpcomingReturnReminders();
        doThrow(new RuntimeException("Error 2")).when(loanReminderService).sendOverdueUserReminders();
        doThrow(new RuntimeException("Error 3")).when(loanReminderService).sendOverdueLibrarianAlerts();

        scheduler.processReminders();

        verify(loanReminderService).sendUpcomingReturnReminders();
        verify(loanReminderService).sendOverdueUserReminders();
        verify(loanReminderService).sendOverdueLibrarianAlerts();
    }
}
