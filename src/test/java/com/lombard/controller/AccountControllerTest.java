package com.lombard.controller;

import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты страницы счёта клиента: просмотр пополнение и вывод денег. Сервисы замоканы. Проверяю что контроллер вызывает их с правильными параметрами

@WebMvcTest(controllers = AccountController.class)
class AccountControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void deposit_validAmount_callsService() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/account/deposit").with(csrf())
                        .param("amount", "1000")
                        .param("comment", "test"))
                .andExpect(status().is3xxRedirection());
        verify(accountService).deposit(eq(1L), eq(new BigDecimal("1000")), eq("test"));
    }

    // если сервис выкинул ошибку клиента просто возвращает на страницу
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void deposit_serviceError_redirectsWithMessage() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        doThrow(new IllegalArgumentException("bad")).when(accountService).deposit(any(), any(), any());

        mvc.perform(post("/client/account/deposit").with(csrf())
                        .param("amount", "1000"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void withdraw_validAmount_callsService() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/account/withdraw").with(csrf())
                        .param("amount", "500"))
                .andExpect(status().is3xxRedirection());
        verify(accountService).withdraw(eq(1L), eq(new BigDecimal("500")), isNull());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void accountPage_asClient_opens() throws Exception {
        User u = new User(); u.setId(1L); u.setUsername("user");
        Client c = new Client(); c.setId(1L);
        Account a = new Account(c); a.setBalance(new BigDecimal("1000"));

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(accountService.getOrCreateForClient(c)).thenReturn(a);
        when(accountService.getHistory(any())).thenReturn(java.util.List.of());

        mvc.perform(get("/client/account")).andExpect(status().isOk());
    }
}
