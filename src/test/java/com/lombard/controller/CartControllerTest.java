package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты корзины клиента: просмотр, добавление и удаление товаров
@WebMvcTest(controllers = CartController.class)
class CartControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void cartPage_withAccount_opens() throws Exception {
        User u = new User(); u.setId(1L); u.setUsername("user");
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(cartService.getByClient(1L)).thenReturn(List.of());
        com.lombard.model.Account acc = new com.lombard.model.Account(c);
        acc.setBalance(new BigDecimal("1000"));
        when(accountService.getOrCreateForClient(c)).thenReturn(acc);

        mvc.perform(get("/client/cart")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void addToCart_loanOnSale_redirects() throws Exception {
        User u = new User(); u.setId(1L); u.setUsername("user");
        Client c = new Client(); c.setId(1L);
        com.lombard.model.Loan l = new com.lombard.model.Loan();
        l.setId(5L); l.setStatus("На продаже");
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(5L)).thenReturn(l);

        mvc.perform(post("/client/cart/add/5").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void cartPage_emptyCart_opens() throws Exception {
        User u = new User(); u.setId(1L);
        com.lombard.model.Client c = new com.lombard.model.Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(get("/client/cart")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void removeFromCart_ownItem_removesAndRedirects() throws Exception {
        User u = new User(); u.setId(1L);
        com.lombard.model.Client c = new com.lombard.model.Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(post("/client/cart/remove/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/cart"));

        verify(cartService).remove(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void addToCart_loanNotFound_redirects() throws Exception {
        User u = new User(); u.setId(1L);
        Client cl = new Client(); cl.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(cl);
        when(loanService.findById(99L)).thenReturn(null);

        mvc.perform(post("/client/cart/add/99").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // если счёт почему-то не нашёлся, корзина открывается с нулевым балансом
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void cartPage_noAccount_opensWithZeroBalance() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(cartService.getByClient(1L)).thenReturn(List.of());
        when(accountService.getOrCreateForClient(c)).thenReturn(null);

        mvc.perform(get("/client/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("client_cart"))
                .andExpect(model().attribute("canPay", true));
    }
}
