package com.lombard.controller.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Проверка аннотации валидации на формах (@NotBlank @Pattern итд) через обычный Validator.
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void registerForm_validData_noErrors() {
        RegisterForm f = new RegisterForm();
        f.setUsername("user1");
        f.setPassword("password123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setMiddleName("Иванович");
        f.setEmail("u@test.com");
        f.setPhone("+79991112233");
        assertTrue(validator.validate(f).isEmpty());
        assertEquals("user1", f.getUsername());
        assertEquals("password123", f.getPassword());
        assertEquals("Иванов", f.getLastName());
        assertEquals("Иван", f.getFirstName());
        assertEquals("Иванович", f.getMiddleName());
        assertEquals("u@test.com", f.getEmail());
        assertEquals("+79991112233", f.getPhone());
    }

    @Test
    void registerForm_badUsername_hasErrors() {
        RegisterForm f = new RegisterForm();
        f.setUsername("ab");
        f.setPassword("password123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setEmail("u@test.com");
        f.setPhone("+79991112233");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void registerForm_shortPassword_hasErrors() {
        RegisterForm f = new RegisterForm();
        f.setUsername("user1");
        f.setPassword("123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setEmail("u@test.com");
        f.setPhone("+79991112233");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void registerForm_badPhone_hasErrors() {
        RegisterForm f = new RegisterForm();
        f.setUsername("user1");
        f.setPassword("password123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setEmail("u@test.com");
        f.setPhone("8999");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void registerForm_badEmail_hasErrors() {
        RegisterForm f = new RegisterForm();
        f.setUsername("user1");
        f.setPassword("password123");
        f.setLastName("Иванов");
        f.setFirstName("Иван");
        f.setEmail("not-an-email");
        f.setPhone("+79991112233");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void checkoutForm_validData_noErrors() {
        CheckoutForm f = new CheckoutForm();
        f.setDeliveryType("PICKUP");
        f.setContactPhone("+79991112233");
        assertTrue(validator.validate(f).isEmpty());
    }

    @Test
    void checkoutForm_unknownDeliveryType_hasErrors() {
        CheckoutForm f = new CheckoutForm();
        f.setDeliveryType("OTHER");
        f.setContactPhone("+79991112233");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void checkoutForm_badPhone_hasErrors() {
        CheckoutForm f = new CheckoutForm();
        f.setDeliveryType("PICKUP");
        f.setContactPhone("8999");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void checkoutForm_addressFields_getSameValues() {
        CheckoutForm f = new CheckoutForm();
        f.setCity("M");
        f.setStreet("S");
        f.setBuilding("B");
        f.setApartment("A");
        f.setPickupAddress("P");
        f.setComment("C");
        assertEquals("M", f.getCity());
        assertEquals("S", f.getStreet());
        assertEquals("B", f.getBuilding());
        assertEquals("A", f.getApartment());
        assertEquals("P", f.getPickupAddress());
        assertEquals("C", f.getComment());
    }

    @Test
    void loanCreateForm_validData_noErrors() {
        LoanCreateForm f = new LoanCreateForm();
        f.setAmount(new BigDecimal("100"));
        f.setMonths(12);
        f.setCollateralName("Товар");
        f.setCollateralDescription("Описание");
        assertTrue(validator.validate(f).isEmpty());
    }

    @Test
    void loanCreateForm_badAmount_hasErrors() {
        LoanCreateForm f = new LoanCreateForm();
        f.setAmount(new BigDecimal("0.50"));
        f.setMonths(12);
        f.setCollateralName("Товар");
        f.setCollateralDescription("Описание");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void loanCreateForm_badMonths_hasErrors() {
        LoanCreateForm f = new LoanCreateForm();
        f.setAmount(new BigDecimal("100"));
        f.setMonths(30);
        f.setCollateralName("Товар");
        f.setCollateralDescription("Описание");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void loanCreateForm_blankName_hasErrors() {
        LoanCreateForm f = new LoanCreateForm();
        f.setAmount(new BigDecimal("100"));
        f.setMonths(12);
        f.setCollateralName("");
        f.setCollateralDescription("Описание");
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void loanEditForm_validData_noErrors() {
        LoanEditForm f = new LoanEditForm();
        f.setAmount(new BigDecimal("100"));
        f.setRate(new BigDecimal("15.5"));
        f.setStatus("Выдан");
        f.setDueDate("2026-01-01");
        assertTrue(validator.validate(f).isEmpty());
    }

    @Test
    void loanEditForm_badRate_hasErrors() {
        LoanEditForm f = new LoanEditForm();
        f.setAmount(new BigDecimal("100"));
        f.setRate(new BigDecimal("150"));
        assertFalse(validator.validate(f).isEmpty());
    }

    @Test
    void loanEditForm_negativeAmount_hasErrors() {
        LoanEditForm f = new LoanEditForm();
        f.setAmount(new BigDecimal("-5"));
        assertFalse(validator.validate(f).isEmpty());
    }
}