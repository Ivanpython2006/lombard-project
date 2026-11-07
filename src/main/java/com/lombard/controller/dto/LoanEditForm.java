package com.lombard.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LoanEditForm {

    @DecimalMin(value = "0.01", message = "Сумма > 0")
    private BigDecimal amount;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00", message = "Ставка 0–100%")
    private BigDecimal rate;

    private String status;

    private String dueDate;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}