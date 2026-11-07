package com.lombard.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Каждую ночь проверяю займы: шлю напоминания, перевожу в просроченные и на склад.
 * Раньше это делалось только при открытии списка займов менеджером,
 * теперь статусы обновляются даже если никто не заходил на сайт.
 */
@Component
public class OverdueLoanScheduler {

    private final LoanService loanService;
    private final NotificationService notificationService;

    public OverdueLoanScheduler(LoanService loanService, NotificationService notificationService) {
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 1 * * *") // каждый день в 01:00
    public void checkOverdueLoans() {
        loanService.processOverdueLoans(notificationService);
    }
}
