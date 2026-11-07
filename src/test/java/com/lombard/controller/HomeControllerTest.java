package com.lombard.controller;

import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты главной страницы: у менеджера и у клиента она разная
@WebMvcTest(controllers = HomeController.class)
class HomeControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void homePage_asManager_opens() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    // у клиента на главной показываются активные займы и баланс
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void homePage_clientWithActiveLoan_opens() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Account a = new Account(c); a.setBalance(new BigDecimal("1000"));

        Loan l = new Loan(); l.setAmount(new BigDecimal("1000")); l.setRate(new BigDecimal("10"));
        l.setStatus("Выдан"); l.setDueDate(LocalDate.now().plusDays(5));

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(accountService.getOrCreateForClient(c)).thenReturn(a);
        when(loanService.findByClientId(1L)).thenReturn(List.of(l));

        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void homePage_clientWithoutProfile_opens() throws Exception {
        User u = new User(); u.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(null);

        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
