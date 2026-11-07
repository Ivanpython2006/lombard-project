package com.lombard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Ночная задача просто вызывает обработку просрочек в сервисе займов.
 */
@ExtendWith(MockitoExtension.class)
class OverdueLoanSchedulerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OverdueLoanScheduler scheduler;

    @Test
    void checkOverdueLoans_callsLoanService() {
        scheduler.checkOverdueLoans();

        verify(loanService).processOverdueLoans(notificationService);
    }
}
