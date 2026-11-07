package com.lombard.controller.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Проверка геттеры и сеттеры форм (DTO) которые приходят из HTML-форм
class DtoAccessorsTest {

    @Test
    void loanCreateForm_setValues_getSameValues() {
        LoanCreateForm f = new LoanCreateForm();
        f.setAmount(new BigDecimal("100.00"));
        f.setMonths(12);
        f.setCollateralName("Телефон");
        f.setCollateralDescription("Описание");

        assertEquals(new BigDecimal("100.00"), f.getAmount());
        assertEquals(12, f.getMonths());
        assertEquals("Телефон", f.getCollateralName());
        assertEquals("Описание", f.getCollateralDescription());
    }

    @Test
    void loanEditForm_setValues_getSameValues() {
        LoanEditForm f = new LoanEditForm();
        f.setAmount(new BigDecimal("500.00"));
        f.setRate(new BigDecimal("10.5"));
        f.setStatus("Выдан");
        f.setDueDate("2026-12-31");

        assertEquals(new BigDecimal("500.00"), f.getAmount());
        assertEquals(new BigDecimal("10.5"), f.getRate());
        assertEquals("Выдан", f.getStatus());
        assertEquals("2026-12-31", f.getDueDate());
    }

    @Test
    void registerForm_setValues_getSameValues() {
        RegisterForm f = new RegisterForm();
        f.setUsername("user123");
        f.setPassword("password123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setMiddleName("Иванович");
        f.setEmail("test@test.com");
        f.setPhone("+79991234567");

        assertEquals("user123", f.getUsername());
        assertEquals("password123", f.getPassword());
        assertEquals("Иванов", f.getLastName());
        assertEquals("Иван", f.getFirstName());
        assertEquals("Иванович", f.getMiddleName());
        assertEquals("test@test.com", f.getEmail());
        assertEquals("+79991234567", f.getPhone());
    }

    @Test
    void checkoutForm_setValues_getSameValues() {
        CheckoutForm f = new CheckoutForm();
        f.setDeliveryType("DELIVERY");
        f.setCity("Москва");
        f.setStreet("Ленина");
        f.setBuilding("1");
        f.setApartment("2");
        f.setPickupAddress("Адрес");
        f.setContactPhone("+79991234567");
        f.setComment("Комментарий");

        assertEquals("DELIVERY", f.getDeliveryType());
        assertEquals("Москва", f.getCity());
        assertEquals("Ленина", f.getStreet());
        assertEquals("1", f.getBuilding());
        assertEquals("2", f.getApartment());
        assertEquals("Адрес", f.getPickupAddress());
        assertEquals("+79991234567", f.getContactPhone());
        assertEquals("Комментарий", f.getComment());
    }
}