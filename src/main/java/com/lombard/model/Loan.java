package com.lombard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", unique = true, length = 100)
    private String loanNumber;

    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal rate; // ставка в процентах

    @Column(length = 50)
    private String status;

    private LocalDate createdDate; // когда создан
    private LocalDate dueDate; // вернуть до
    private LocalDate decisionDate; // когда менеджер принял решение

    @Column(name = "client_approved")
    private Boolean clientApproved;

    @Column(name = "agreed_amount", precision = 19, scale = 2)
    private BigDecimal agreedAmount;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true) // один заем может иметь несколько залогов
    private List<Collateral> collaterals = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdDate == null) { // если дата создания не задана ставлю текущую
            createdDate = LocalDate.now();
        }
    }

    public Loan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDate decisionDate) { this.decisionDate = decisionDate; }

    public Boolean getClientApproved() { return clientApproved; }
    public void setClientApproved(Boolean clientApproved) { this.clientApproved = clientApproved; }

    public BigDecimal getAgreedAmount() { return agreedAmount; }
    public void setAgreedAmount(BigDecimal agreedAmount) { this.agreedAmount = agreedAmount; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<Collateral> getCollaterals() { return collaterals; }
    public void setCollaterals(List<Collateral> collaterals) { this.collaterals = collaterals; }

    // считаю итоговую сумму: сумма + процент с округлением до копеек
    public BigDecimal getTotalToReturn() {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        BigDecimal ratePart = amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return amount.add(ratePart).setScale(2, RoundingMode.HALF_UP);
    }

    // размер переплаты (только процент)
    public BigDecimal getOverpayment() {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        return amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // проверка просрочки (только для активных займов)
    public boolean isOverdue() {
        return "Выдан".equals(status)
                && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    // сколько дней осталось до возврата
    public long getDaysLeft() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    // сколько дней прошло с момента создания
    public long getDaysPassed() {
        if (createdDate == null) return 0;
        return ChronoUnit.DAYS.between(createdDate, LocalDate.now());
    }

    // цвет таймера в зависимости от оставшегося времени
    public String getTimerClass() {
        if ("Закрыт".equals(status)
                || "Отклонен".equals(status)
                || "Отменена".equals(status)
                || "На продаже".equals(status)
                || "Куплен".equals(status)
                || "На складе".equals(status)) {
            return "";
        }
        long days = getDaysLeft();
        if (days < 0) return "timer-danger";
        if (days <= 3) return "timer-danger";
        if (days <= 7) return "timer-warning";
        return "timer-success";
    }

    // текст под таймером: дни "сегодня последний день" "просрочено на N"
    public String getTimerLabel() {
        if (dueDate == null) return "-";
        long days = getDaysLeft();
        if (days < 0) {
            long overdue = -days;
            return "Просрочено на " + overdue + " " + pluralDays(overdue);
        }
        if (days == 0) return "Последний день";
        if (days == 1) return "Остался 1 день";
        return "Осталось " + days + " " + pluralDays(days);
    }

    // прогресс-бар (сколько прошло от срока)
    public int getProgressPercent() {
        if (createdDate == null || dueDate == null) return 0;
        long total = ChronoUnit.DAYS.between(createdDate, dueDate);
        if (total <= 0) return 100;
        long passed = ChronoUnit.DAYS.between(createdDate, LocalDate.now());
        if (passed < 0) return 0;
        if (passed > total) return 100;
        return (int) (passed * 100 / total);
    }

    //склонения: день дня дней
    private String pluralDays(long n) {
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "день";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return "дня";
        return "дней";
    }
}