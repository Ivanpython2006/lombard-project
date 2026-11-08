package com.lombard.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Тесты вычисляемых полей займа: сумма к возврату, просрочка, таймер до конца срока и прогресс-бар (их показывают в личном кабинете).

class LoanTest {

    // заем со статусом «Выдан» и сроком возврата через daysToDue дней
    private Loan issued(int daysToDue) {
        Loan l = new Loan();
        l.setStatus("Выдан");
        l.setDueDate(LocalDate.now().plusDays(daysToDue));
        return l;
    }

    // 1000 руб под 10% => вернуть 1100 переплата 100
    @Test
    void totalToReturn_amountAndRate_addsPercent() {
        Loan loan = new Loan();
        loan.setAmount(new BigDecimal("1000"));
        loan.setRate(new BigDecimal("10"));

        assertEquals(new BigDecimal("1100.00"), loan.getTotalToReturn());
        assertEquals(new BigDecimal("100.00"), loan.getOverpayment());
    }

    @Test
    void totalToReturn_noAmount_returnsZero() {
        Loan loan = new Loan();

        assertEquals(BigDecimal.ZERO, loan.getTotalToReturn());
        assertEquals(BigDecimal.ZERO, loan.getOverpayment());
    }

    @Test
    void isOverdue_issuedAndPastDue_returnsTrue() {
        assertTrue(issued(-2).isOverdue());
    }

    // закрытый заем не может быть просроченным даже если дата прошла
    @Test
    void isOverdue_closedLoan_returnsFalse() {
        Loan l = issued(-2);
        l.setStatus("Закрыт");

        assertFalse(l.isOverdue());
    }

    // цвет таймера: зеленый - много времени, желтый - меньше недели, красный - 3 дня и меньше
    @Test
    void timerClass_differentDaysLeft_returnsColor() {
        assertEquals("timer-success", issued(10).getTimerClass());
        assertEquals("timer-warning", issued(5).getTimerClass());
        assertEquals("timer-danger", issued(2).getTimerClass());
        assertEquals("timer-danger", issued(-2).getTimerClass());
    }

    @Test
    void timerClass_closedLoan_isEmpty() {
        Loan l = issued(10);
        l.setStatus("Закрыт");

        assertEquals("", l.getTimerClass());
        assertNotNull(l.getTimerLabel());
    }

    @Test
    void timerLabel_differentDaysLeft_returnsText() {
        assertEquals("Последний день", issued(0).getTimerLabel());
        assertEquals("Остался 1 день", issued(1).getTimerLabel());
        assertTrue(issued(2).getTimerLabel().startsWith("Осталось 2"));
        assertEquals("2 days left", issued(2).getTimerLabelEn());
        assertEquals("Last day", issued(0).getTimerLabelEn());
        assertEquals("1 day left", issued(1).getTimerLabelEn());
        assertEquals("Overdue by 5 days", issued(-5).getTimerLabelEn());
        assertEquals("Overdue by 1 day", issued(-1).getTimerLabelEn());
        assertTrue(issued(-5).getTimerLabel().startsWith("Просрочено на 5"));
    }

    @Test
    void timerLabel_noDueDate_returnsDash() {
        Loan l = issued(0);
        l.setDueDate(null);

        assertEquals("-", l.getTimerLabel());
        assertEquals("-", l.getTimerLabelEn());
    }

    @Test
    void daysLeftAndPassed_createdTenDaysAgo_countsDays() {
        Loan loan = issued(5);
        loan.setCreatedDate(LocalDate.now().minusDays(10));

        assertEquals(5, loan.getDaysLeft());
        assertEquals(10, loan.getDaysPassed());
    }

    // прошла половина срока => 50% срок вышел => 100% еще не начался => 0%
    @Test
    void progressPercent_differentDates_returnsPercent() {
        Loan l = new Loan();
        l.setCreatedDate(LocalDate.now().minusDays(10));
        l.setDueDate(LocalDate.now().plusDays(10));
        assertEquals(50, l.getProgressPercent());

        l.setDueDate(LocalDate.now().minusDays(5));
        assertEquals(100, l.getProgressPercent());

        l.setCreatedDate(LocalDate.now().plusDays(5));
        l.setDueDate(LocalDate.now().plusDays(20));
        assertEquals(0, l.getProgressPercent());
    }

    @Test
    void prePersist_newLoan_setsCreatedDate() {
        Loan l = new Loan();

        l.prePersist();

        assertNotNull(l.getCreatedDate());
    }
}
