package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты магазина: витрина для клиента и управление товарами для менеджера
@WebMvcTest(controllers = ShopController.class)
class ShopControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_noItems_returnsCsv() throws Exception {
        when(loanService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/shop/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manage_allSortOptions_open() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("На складе");
        l.setCollaterals(new ArrayList<>());

        when(loanService.findAll()).thenReturn(List.of(l));

        mvc.perform(get("/shop/manage").param("sort", "date_asc")).andExpect(status().isOk());
        mvc.perform(get("/shop/manage").param("sort", "price_desc")).andExpect(status().isOk());
        mvc.perform(get("/shop/manage").param("sort", "price_asc")).andExpect(status().isOk());
        mvc.perform(get("/shop/manage").param("sort", "item")).andExpect(status().isOk());
        mvc.perform(get("/shop/manage").param("query", "test")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manage_withFilters_opens() throws Exception {
        when(loanService.findAll()).thenReturn(List.of());
        mvc.perform(get("/shop/manage")
                        .param("status", "all")
                        .param("sort", "price_asc")
                        .param("query", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void removeFromSale_itemOnSale_saves() throws Exception {
        when(loanService.findById(1L)).thenReturn(onSaleLoan());
        mvc.perform(post("/shop/1/remove").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(any());
    }

    // редактировать можно только товар со склада
    @Test
    @WithMockUser(roles = "MANAGER")
    void saveItem_notInStock_notSaved() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("На продаже");
        when(loanService.findById(1L)).thenReturn(l);
        mvc.perform(post("/shop/1/save").with(csrf()).param("itemName", "x"))
                .andExpect(status().is3xxRedirection());
        verify(loanService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void publish_itemInStock_saves() throws Exception {
        Loan l = onSaleLoan();
        l.setStatus("На складе");
        when(loanService.findById(1L)).thenReturn(l);
        mvc.perform(post("/shop/1/publish").with(csrf()).param("price", "1000"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void shopPage_asClient_opens() throws Exception {
        mvc.perform(get("/shop")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "CLIENT")
    void addToCart_itemOnSale_callsCartService() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Loan l = new Loan(); l.setId(1L); l.setStatus("На продаже");

        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/shop/1/add-to-cart").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(cartService).add(c, l);
    }

    private Loan onSaleLoan() {
        Loan l = new Loan();
        l.setId(1L); l.setStatus("На продаже");
        Collateral c = new Collateral();
        l.setCollaterals(new ArrayList<>(List.of(c)));
        return l;
    }
}
