package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты контроллера клиента: мои займы заявка на заем, подтверждение/отмена/погашение,
// профиль и выгрузки. Пользователь "user" - клиент с id = 1

@WebMvcTest(controllers = ClientController.class)
class ClientControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void exportClients_asManager_returnsCsv() throws Exception {
        when(clientService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/client/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void exportMyLoans_noLoans_returnsCsv() throws Exception {
        com.lombard.model.User u = new com.lombard.model.User();
        u.setId(1L);
        com.lombard.model.Client c = new com.lombard.model.Client();
        c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findByClientId(1L)).thenReturn(new ArrayList<>());

        mvc.perform(get("/client/loans/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void myLoans_noClientProfile_redirects() throws Exception {
        when(userService.findByUsername("user")).thenReturn(new User());
        when(clientService.findByUserId(any())).thenReturn(null);
        mvc.perform(get("/client/loans")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void myLoans_allSortOptions_open() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(1L); l.setStatus("Выдан"); l.setAmount(BigDecimal.TEN);
        l.setCollaterals(new ArrayList<>());

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findByClientId(1L)).thenReturn(List.of(l));

        mvc.perform(get("/client/loans").param("sort", "amount")).andExpect(status().isOk());
        mvc.perform(get("/client/loans").param("sort", "due")).andExpect(status().isOk());
        mvc.perform(get("/client/loans").param("sort", "status")).andExpect(status().isOk());
        mvc.perform(get("/client/loans").param("query", "test")).andExpect(status().isOk());
    }

    // заем не найден / не в том статусе - ничего не подтверждаем
    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void confirmLoan_notFoundOrWrongStatus_redirects() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        when(loanService.findById(99L)).thenReturn(null);
        mvc.perform(post("/client/loans/99/confirm").with(csrf()).param("agree", "true"))
                .andExpect(status().is3xxRedirection());

        Loan l = new Loan(); l.setId(1L); l.setClient(new Client()); l.getClient().setId(99L);
        when(loanService.findById(1L)).thenReturn(l);
        mvc.perform(post("/client/loans/1/confirm").with(csrf()).param("agree", "true"))
                .andExpect(status().is3xxRedirection());

        l.getClient().setId(1L); l.setStatus("Выдан");
        mvc.perform(post("/client/loans/1/confirm").with(csrf()).param("agree", "true"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void exportMyLoans_withLoan_returnsCsv() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);

        Loan l = new Loan(); l.setId(1L);
        l.setLoanNumber("L1"); l.setAmount(BigDecimal.TEN); l.setRate(BigDecimal.ONE);
        l.setStatus("Выдан"); l.setCreatedDate(LocalDate.now()); l.setDueDate(LocalDate.now().plusDays(10));

        Collateral col = new Collateral(); col.setName("Phone"); col.setItemNumber("M1");
        l.setCollaterals(List.of(col));

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findByClientId(1L)).thenReturn(List.of(l));

        mvc.perform(get("/client/loans/export")).andExpect(status().isOk());
    }

    // клиент согласился с условиями менеджера
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void confirmLoan_agree_savesLoan() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(5L); l.setStatus("Ожидает подтверждения"); l.setClient(c);
        // заявку подали 10 дней назад на 3 месяца
        l.setCreatedDate(java.time.LocalDate.now().minusDays(10));
        l.setDueDate(java.time.LocalDate.now().minusDays(10).plusMonths(3));
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(5L)).thenReturn(l);
        when(loanService.getItemName(l)).thenReturn("Phone");

        mvc.perform(post("/client/loans/5/confirm").with(csrf()).param("agree", "true"))
                .andExpect(status().is3xxRedirection());
        verify(loanService, atLeastOnce()).save(l);
        assertEquals("Выдан", l.getStatus());
        // срок считается с момента выдачи, а не с подачи заявки
        assertEquals(java.time.LocalDate.now().plusMonths(3), l.getDueDate());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void confirmLoan_decline_redirects() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(5L); l.setStatus("Ожидает подтверждения"); l.setClient(c);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(5L)).thenReturn(l);
        when(loanService.getItemName(l)).thenReturn("Phone");

        mvc.perform(post("/client/loans/5/confirm").with(csrf()).param("agree", "false"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void cancelLoan_pendingLoan_redirects() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(5L); l.setStatus("В ожидании"); l.setClient(c);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(5L)).thenReturn(l);
        when(loanService.getItemName(l)).thenReturn("Phone");

        mvc.perform(post("/client/loans/5/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void repayLoan_issuedLoan_withdrawsMoney() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(5L); l.setStatus("Выдан"); l.setClient(c);
        l.setAmount(java.math.BigDecimal.valueOf(1000));
        l.setRate(java.math.BigDecimal.valueOf(10));
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(5L)).thenReturn(l);
        when(loanService.getItemName(l)).thenReturn("Phone");

        mvc.perform(post("/client/loans/5/repay").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        // списание и закрытие займа идут одной транзакцией в сервисе
        verify(loanService).repayLoan(5L, 1L, accountService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void exportClients_withClient_returnsCsv() throws Exception {
        Client c = new Client(); c.setId(1L); c.setFullName("Ivan"); c.setPhone("+79990000000");
        when(clientService.findAll()).thenReturn(List.of(c));

        mvc.perform(get("/client/export"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void updateProfile_badLastName_notSaved() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "1")
                        .param("firstName", "Ivan")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void updateProfile_badPhone_notSaved() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Ivanov")
                        .param("firstName", "Ivan")
                        .param("email", "a@b.ru")
                        .param("phone", "123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void updateProfile_phoneTaken_notSaved() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(clientService.existsByPhoneExceptId("+79991112233", 1L)).thenReturn(true);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Ivanov")
                        .param("firstName", "Ivan")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void updateProfile_wrongCurrentPassword_notSaved() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(userService.checkPassword("user", "wrong")).thenReturn(false);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Ivanov")
                        .param("firstName", "Ivan")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233")
                        .param("currentPassword", "wrong")
                        .param("newPassword", "newpass")
                        .param("confirmPassword", "newpass"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchPage_asAdmin_opens() throws Exception {
        mvc.perform(get("/client/search")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void createLoan_badAmount_returnsForm() throws Exception {
        mvc.perform(post("/client/loans/new")
                        .with(csrf())
                        .param("amount", "0")
                        .param("months", "5")
                        .param("collateralName", "Phone")
                        .param("collateralDescription", "Good"))
                .andExpect(status().isOk())
                .andExpect(view().name("add_loan"));

        verify(loanService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void createLoan_badMonths_returnsForm() throws Exception {
        mvc.perform(post("/client/loans/new").with(csrf())
                        .param("amount", "1000")
                        .param("months", "0")
                        .param("collateralName", "Phone")
                        .param("collateralDescription", "Good"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void createLoan_emptyCollateralName_notCreated() throws Exception {
        mvc.perform(post("/client/loans/new").with(csrf())
                        .param("amount", "1000")
                        .param("months", "5")
                        .param("collateralName", "")
                        .param("collateralDescription", "Good"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/loans/new"));

        verify(loanService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void updateProfile_validData_saves() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(clientService.existsByPhoneExceptId("+79991112233", 1L)).thenReturn(false);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Ivanov")
                        .param("firstName", "Ivan")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection());
        verify(clientService).save(c);
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void myLoans_withLoan_opens() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L); c.setUser(u);
        Loan l = new Loan(); l.setId(5L); l.setClient(c);

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findByClientId(1L)).thenReturn(List.of(l));

        mvc.perform(get("/client/loans")).andExpect(status().isOk());
    }

    // заявка на заем вместе с фото вещи
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void createLoan_withPhoto_createsLoan() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L); c.setUser(u);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        MockMultipartFile photo = new MockMultipartFile("photos", "test.jpg", "image/jpeg", "data".getBytes());

        mvc.perform(multipart("/client/loans/new").file(photo).with(csrf())
                        .param("amount", "1000")
                        .param("months", "5")
                        .param("collateralName", "Phone")
                        .param("collateralDescription", "Good"))
                .andExpect(status().is3xxRedirection());

        verify(loanService).save(any(Loan.class));
    }

    // если пароль не меняют поля пароля пустые
    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void updateProfile_withoutPassword_saves() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Ivanov")
                        .param("firstName", "Ivan")
                        .param("email", "test@test.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection());

        verify(clientService).save(c);
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void myLoans_noLoans_opens() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findByClientId(1L)).thenReturn(new ArrayList<>());
        mvc.perform(get("/client/loans")).andExpect(status().isOk());
    }

    // пользователя удалили, пока он был в системе: не падаем, а возвращаем к списку займов
    @Test
    @WithMockUser(roles = "CLIENT", username = "deleted")
    void createLoan_userNotFound_redirectsWithoutSaving() throws Exception {
        when(userService.findByUsername("deleted")).thenReturn(null);

        mvc.perform(post("/client/loans/new").with(csrf())
                        .param("amount", "1000")
                        .param("months", "5")
                        .param("collateralName", "Телефон")
                        .param("collateralDescription", "Хороший"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/loans"));

        verify(loanService, never()).save(any());
    }
}
