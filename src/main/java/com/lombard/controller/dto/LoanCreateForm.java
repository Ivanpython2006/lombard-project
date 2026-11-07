package com.lombard.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LoanCreateForm {

    @NotNull
    @DecimalMin(value = "1.00", message = "Сумма минимум 1")
    private BigDecimal amount;

    @NotNull
    @Min(value = 1, message = "Срок от 1 мес.")
    @Max(value = 24, message = "Срок до 24 мес.")
    private Integer months;

    @NotBlank(message = "Название вещи обязательно")
    @Size(max = 200)
    private String collateralName;

    @NotBlank(message = "Описание обязательно")
    @Size(max = 2000)
    private String collateralDescription;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getMonths() { return months; }
    public void setMonths(Integer months) { this.months = months; }
    public String getCollateralName() { return collateralName; }
    public void setCollateralName(String collateralName) { this.collateralName = collateralName; }
    public String getCollateralDescription() { return collateralDescription; }
    public void setCollateralDescription(String collateralDescription) { this.collateralDescription = collateralDescription; }
}